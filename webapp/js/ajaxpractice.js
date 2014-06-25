function ajaxAsyncRequest(reqURL)
{
    //Creating a new XMLHttpRequest object
    var xmlhttp;
    if (window.XMLHttpRequest){
        xmlhttp = new XMLHttpRequest(); //for IE7+, Firefox, Chrome, Opera, Safari
    } else {
        xmlhttp = new ActiveXObject("Microsoft.XMLHTTP"); //for IE6, IE5
    }
    //Create a asynchronous GET request
    xmlhttp.open("GET", reqURL, true);
     
    //When readyState is 4 then get the server output
    xmlhttp.onreadystatechange = function() {
        if (xmlhttp.readyState == 4) { 
            if (xmlhttp.status == 200) 
            {
                document.getElementById("message").innerHTML = xmlhttp.responseText;
                //alert(xmlhttp.responseText);
            } 
            else
            {
                alert('Something is wrong !!');
            }
        }
    };
     
    xmlhttp.send(null);
}
