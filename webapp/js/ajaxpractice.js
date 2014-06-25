$(document).ready(function() {
    console.log("script run?");                                     
    $("#myButton").click(function(e) {
        console.log("entered message button click...");                     
        $.ajax({
            type: "POST",
            url: "HelloWorld",
            data: "name=andrew",
            dataType: "json",

            success: function( data, textStatus, jqXHR) {
                if(data.success) {
                    $("#ajaxResponse").html("");
                    $("#ajaxResponse").append("<b>Name:</b> " + data.nameInfo.nameSt);
                } 

                else {
                    $("#ajaxResponse").html("<div><b>Country code in Invalid!</b></div>");
                }
            },

            error: function(jqXHR, textStatus, errorThrown){
                 console.log("Something really bad happened " + textStatus);
                  $("#ajaxResponse").html(jqXHR.responseText);
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
