$('#uploadButton').click(function(){
    var formData = new FormData($('form')[0]);
    console.log("running upload");
    $.ajax({
        type: 'POST',
        url: 'RunLabelPropagation',
        data: formData,
        dataType: "json",

        success: function( data, textStatus, jqXHR) {
            if(data.success) {
                console.log("java says success");
                cy = cytoscape( options = {
                    container: document.getElementById('cy')
                });
                cy.load(data);
                $("#ajaxResponse").html("<div>" + JSON.stringify(data) + "</div>");
            } else {
                console.log("java says failure");
                console.log(data.error);
                $("#ajaxResponse").html("<div><b>Failed to process graph</b></div>");
            }
        },

        beforeSend: function(jqXHR, settings) {
            $('#myButton').attr("disabled", true);
        },

        complete: function(jqXHR, textStatus){
            $('#myButton').attr("disabled", false);
        },

        error: function(jqXHR, textStatus, errorThrown){
            console.log("Something really bad happened " + textStatus);
            $("#ajaxResponse").html("<div><b>Failed to process graph: POST error</b></div>");
        },
        
        cache: false,
        contentType: false,
        processData: false
    });
});
