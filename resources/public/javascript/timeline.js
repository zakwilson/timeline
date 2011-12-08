var tg1;

$(document).ready(function () { 

    tg1 = $("#placement").timeline({
        "min_zoom":15, 
        "max_zoom":70, 
        "data_source":"/timeline",
        "event_modal":{"type": "normal",
                       "href": "/html/modal.html"}
    });
    
});

function edit (id){
    alert(id);
}