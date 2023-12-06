(function () {
  'use strict';
    /*
    HTMX extensions for being compatible with CAUSEWAY-REST-OBJECT HTTP API

    1. Includes pre and post processing of JSON formatted request & responses
    2. Client side MUSTACHE based templating to render HTML
    3. Handling of multiple templates based on server HTTP Response Codes
    4. TODO: 401: Auth handler to redirect user to login.

    */
  htmx.defineExtension('causeway-client-htmx-ro-spec', {
      onEvent: function (name, evt) {
          if (name === "htmx:configRequest") {
                //Response headers as per RO SPEC
              evt.detail.headers['Content-Type'] = "application/json";//Request type
              evt.detail.headers['Accept'] = 'application/json;profile="urn:org.apache.causeway/v2";suppress=ro'; // response Accept Type
              //FYI : https://causeway.apache.org/refguide/2.0.0-RC1/applib/index/client/SuppressionType.html
          }else if( name === 'htmx:beforeSwap'){
             //Response parsing to allow HTMX swaps in case of error response codes too, matching with RO SPEC
              if(evt.detail.xhr.status == 404){
                    //empty response
                    evt.detail.shouldSwap = true;
                    evt.detail.isError = false;
                    //TODO evt.detail.target = htmx.find("#empty");
              }else if(evt.detail.xhr.status == 422){
                      //empty response
                      evt.detail.shouldSwap = true;
                      evt.detail.isError = false;
              }else if(evt.detail.xhr.status >= 400){
                      // allow 4xx responses to swap as we are using this as a signal that
                      // a server submitted with bad data and want to rerender with the
                      // errors
                      //
                      // set isError to false to avoid error logging in console
                      evt.detail.shouldSwap = true;
                      evt.detail.isError = false;
              } else if(evt.detail.xhr.status >= 500){
                  // server side technical error
                  evt.detail.shouldSwap = true;
                  //evt.detail.target = htmx.find("#" + (htmx.closest(evt.detail.elt, "[error-template-target]")).getAttribute(templateAttributeName));
              }
          }
      },

      encodeParameters : function(xhr, parameters, elt) {
            //Formats forms data to RO SPEC Json
          xhr.overrideMimeType('text/json');
          if (elt.attributes['hx-put']) {
           if (elt.attributes['hx-put'].value.includes('/properties/')) {
            var req = {};
            for (let key in parameters) {
                req['value'] = parameters[key];
            }
            return JSON.stringify(req);
           }
         } else {
            var transformedRequest= {};
            for(let key in parameters) {
              transformedRequest[key]={'value':parameters[key]};
            }
            return (JSON.stringify(transformedRequest));
          }
      },

      transformResponse : function(text, xhr, elt) {
               //ALLOWS rendering different templates based on response code
              var templateType = xhr.status == 200?'success':(xhr.status == 404?'empty':'error');
              var templateAttributeName = templateType+'-template';
              var mustacheTemplate = htmx.closest(elt, "["+templateAttributeName+"]");
              if (mustacheTemplate) {
                  var data = JSON.parse(text);
                  var templateId = mustacheTemplate.getAttribute(templateAttributeName);
                   var template = htmx.find("#" + templateId);
                  if (template) {
                      return Mustache.render(template.innerHTML, data);
                  } else {
                      throw "Unknown mustache template: " + templateId;
                  }
              }
      }
  });


})();
