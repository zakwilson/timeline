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