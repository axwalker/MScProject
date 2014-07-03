$('#uploadButton').click(function(){
    window.location.hash = "Status";
    var formData = new FormData($('form')[0]);
    $.ajax({
        type: 'POST',
        url: 'ProcessGraph',
        data: formData,
        dataType: "json",

        success: function( data, textStatus, jqXHR) {
            if(data.success) {
                $("#ajaxResponse").append("<li>>: Graph processed.</li>");
                graphs = data.graphs;
                console.log("in success: " + JSON.stringify(graphs, undefined, 2));
                var metadata = graphs.HighLevel.metadata;
                initHigh(graphs.HighLevel, metadata.MinCommunitySize, metadata.MaxCommunitySize);
            } else {
                console.log(data.error);
                $("#ajaxResponse").append("<li><b>>: Exception: failed to process graph</b></li>");
            }
        },

        beforeSend: function(jqXHR, settings) {
            $('#uploadButton').attr("disabled", true);
            $("#ajaxResponse").append("<li>>: Uploading graph...</li>");
        },

        complete: function(jqXHR, textStatus){
            $('#uploadButton').attr("disabled", false);
        },

        error: function(jqXHR, textStatus, errorThrown){
            console.log("Something really bad happened " + textStatus);
            $("#ajaxResponse").append("<li><b>>: Failed to process graph: POST error</b></li>");
        },
        
        cache: false,
        contentType: false,
        processData: false
    });
});
