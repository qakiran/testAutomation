/*
 * Copyright (c) 2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
<script type="text/javascript">

var id = 'jsonWrapperTest';
var tableSuccess = function(responseData){
    var table = '<table class="labkey-data-region labkey-show-borders"><tr>';
    var i, j;

    for(i = 0; i < responseData.metaData.fields.length; i++){
        var field = responseData.metaData.fields[i];
        table = table + '<th><span>' + field.caption + '</span></th>';
    }

    table = table + '</tr>';

    for(i = 0; i < responseData.rows.length; i++){
        table = table + '<tr>';
        var row = responseData.rows[i];
        for(j = 0; j < responseData.metaData.fields.length; j++){
            var field = responseData.metaData.fields[j];
            var fieldKey = field.fieldKey.toString();
            table = table + '<td><span>' + row[fieldKey].value + '</span></td>';
        }
        table = table + '</tr>';
    }

    table = table + '</table>';
    document.getElementById(id).innerHTML = table;
};

var fancyRequest = REPLACEMENT_STRING

var fReq = LABKEY.Query.GetData.getRawData(fancyRequest);

</script>

<div id="jsonWrapperTest">