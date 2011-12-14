var tg1;

$(document).ready(function () { 
    var include = $("#include").val();

    tg1 = $("#placement").timeline({
        "min_zoom":15, 
        "max_zoom":70, 
        "data_source": include.length == 0 ? "/timeline" : "/timeline?include=" + include,
        "event_modal":{"type": "normal",
                       "href": "/html/modal.html"}
    });
    
});
