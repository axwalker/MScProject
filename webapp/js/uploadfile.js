$('#uploadButton').click(function(){
    var formData = new FormData($('form')[0]);
    console.log("running upload");
    $.ajax({
        type: 'POST',
        url: 'UploadFile',  //Server script to process data
        data: formData,
        dataType: "json",

        beforeSend: function(jqXHR, settings) {
            $('#myButton').attr("disabled", true);
        },

        complete: function(jqXHR, textStatus){
            $('#myButton').attr("disabled", false);
        },

        success: function( data, textStatus, jqXHR) {
            if(data.success) {
                console.log("java says success");
                console.log(data.filename);
            } else {
                console.log("java says failure");
            }
        },

        error: function(jqXHR, textStatus, errorThrown){
            console.log("Something really bad happened " + textStatus);
        },
        //Options to tell jQuery not to process data or worry about content-type.
        cache: false,
        contentType: false,
        processData: false
    });
});
