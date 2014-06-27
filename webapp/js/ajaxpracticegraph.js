$(document).ready(function() {                                   
    $("#myButton").click(function(e) {                    
        $.ajax({
            type: "POST",
            url: "GetGraph",
            dataType: "json",

            success: function( data, textStatus, jqXHR) {
                if(data.success) {
                    cy = cytoscape( options = {
                        container: document.getElementById('cy')
                    });
                    cy.load(data);
                    $("#ajaxResponse").html("<div><b>Sample graph loaded</b></div>");
                } else {
                    $("#ajaxResponse").html("<div><b>Button did not work!</b></div>");
                }
            },

            error: function(jqXHR, textStatus, errorThrown){
                 console.log("Something really bad happened " + textStatus);
                  $("#ajaxResponse").html("error");
            },

            beforeSend: function(jqXHR, settings) {
                $('#myButton').attr("disabled", true);
            },

            complete: function(jqXHR, textStatus){
                $('#myButton').attr("disabled", false);
            }
      
        });        
    });
 
});
