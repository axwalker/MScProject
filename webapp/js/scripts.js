var graphs;

$('#uploadButton').click(function(){
    $("#cy.low").hide();
    $("#cy.high").show();
    window.location.hash = "Status";
    var formData = new FormData($('form')[0]);
    $.ajax({
        type: 'POST',
        url: 'ProcessGraph',
        data: formData,
        processData: false,
        contentType: false,
        dataType: "json",

        success: function( data, textStatus, jqXHR) {
            if(data.success) {
                $("#ajaxResponse").append("<li>>: Graph processed.</li>");
                graphs = data;
                //console.log("in success: " + JSON.stringify(data, undefined, 2));
                var metadata = graphs.metadata;
                initHigh(graphs);
                $("#Modularity span").html(metadata.modularity.toFixed(2));
                $("#MinCommSize span").html(metadata.minCommunitySize);
                $("#MaxCommSize span").html(metadata.maxCommunitySize); 
                $("#HierarchyHeight span").html(metadata.hierarchyHeight); 
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

$('#updateButton').click(function(){
    $("#cy.low").hide();
    $("#cy.high").show();
    window.location.hash = "Status";
    var formData = $("#updateForm").serialize();
    console.log(formData);
    $.ajax({
        type: 'POST',
        url: 'UpdateGraph',
        data: formData,
        dataType: "json",

        success: function( data, textStatus, jqXHR) {
            if(data.success) {
                $("#ajaxResponse").append("<li>>: Graph processed.</li>");
                graphs = data;
                //console.log("in success: " + JSON.stringify(data, undefined, 2));
                var metadata = graphs.metadata;
                initHigh(graphs);
                $("#Modularity span").html(metadata.modularity.toFixed(2));
                $("#MinCommSize span").html(metadata.minCommunitySize);
                $("#MaxCommSize span").html(metadata.maxCommunitySize);
            } else {
                console.log(data.error);
                $("#ajaxResponse").append("<li><b>>: Exception: failed to process graph</b></li>");
            }
        },

        beforeSend: function(jqXHR, settings) {
            $('#updateButton').attr("disabled", true);
            $("#ajaxResponse").append("<li>>: Uploading graph...</li>");
        },

        complete: function(jqXHR, textStatus){
            $('#updateButton').attr("disabled", false);
        },

        error: function(jqXHR, textStatus, errorThrown){
            console.log("Something really bad happened " + textStatus);
            $("#ajaxResponse").append("<li><b>>: Failed to process graph: POST error</b></li>");
        },
        
        cache: false
    });
});

$('#refreshButton').click(function(){
    if (graphs) {
        if ($('#cy.high').is(':visible')) {
            var minCommunitySize = document.getElementById('minCommunitySize').value;
            options = { name: 'null' };
            cyHigh.layout(options);
            cyHigh.load(graphs);
            cyHigh.$('node[size < ' + minCommunitySize + ']').remove();
            cyHigh.layout(highArborLayout());
        }
    }
});