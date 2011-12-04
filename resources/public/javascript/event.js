$(document).ready(function() {
    init_datepicker();
});

function init_datepicker(){
    var oneDay = 24*60*60*1000;
    var rangeDemoFormat = "%Y/%c/%e/%E";
    var rangeDemoConv = new AnyTime.Converter({format:rangeDemoFormat});
    $("#startdate").AnyTime_picker({format:rangeDemoFormat});
    $("#enddate").AnyTime_picker({format:rangeDemoFormat});
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