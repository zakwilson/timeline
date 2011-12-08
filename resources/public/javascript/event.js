$(document).ready(function() {
    init_datepicker();
});

function init_datepicker(){
    var oneDay = 24*60*60*1000;
    var rangeDemoFormat = "%Y/%c/%e";
    var rangeDemoConv = new AnyTime.Converter({format:rangeDemoFormat});
    var picker_args = {format:rangeDemoFormat, askEra:true};
    
    $('#start-date-picker').click(
        function(e) {
            $('#startdate').AnyTime_noPicker().AnyTime_picker(picker_args).focus();
            e.preventDefault();
        } );
    
    $('#end-date-picker').click(
        function(e) {
            $('#enddate').AnyTime_noPicker().AnyTime_picker(picker_args).focus();
            e.preventDefault();
        } );

    $("#enddate").change( function(e) { try {
        var fromDay = rangeDemoConv.parse($("#rangeDemoStart").val()).getTime();
        $("#rangeDemoFinish").
            AnyTime_noPicker().
            removeAttr("disabled").
            val(rangeDemoConv.format(dayLater)).
            AnyTime_picker(
                { format: rangeDemoFormat } );
    } catch(e){ $("#rangeDemoFinish").val("").attr("disabled","disabled"); } } );
}

function fixdate (d){
    if (typeof(d) != "string")
        return d;
    d = d.replace(/-/g, "/");
    // leading - means BCE
    return d.substring(0,1) == "/" ? d.replace("/", "-") : d;
}

function populate (evt){
    $('#id').val(evt.id);
    $('#startdate').val(fixdate(evt.startdate));
    $('#enddate').val(fixdate(evt.enddate));
    $('#evt-form #title').val(evt.title);
    $('#description').val(evt.description);
    $('#link').val(evt.link);
    $('#importance').val(evt.importance);
    $('#tags').val(evt.tags);
}

function edit(ev){
    var id = $(ev).attr('id').substring(3);
    $.ajax({url: "/event/" + id,
            dataType: "json",
            success: populate});
} 

function del(ev){
    var id = $(ev).attr('id').substring(3);
    $.ajax({url: "/event/delete",
            type: "POST",
            data: {id: id},
            success: function () {ev.remove()}});
}