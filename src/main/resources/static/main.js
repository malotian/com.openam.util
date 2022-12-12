var columnDefs = [
	{ title: "id", field: "id" },
	{ title: "type", field: "type" },
	{ title: "INTERNAL-AUTH", field: "INTERNAL_AUTH_LEVEL" },
	{ title: "EXTERNAL-AUTH", field: "EXTERNAL_AUTH_LEVEL" },
	{ title: "ASSIGNED-IDP", field: "ASSIGNED_IDENTITY_PROVIDER" },
	{ title: "SP/IDP", field: "SP/IDP" },
	{ title: "COT", field: "ASSIGNED_COT" },
	{ title: "HOSTED/REMOTE", field: "HOSTED/REMOTE" }
];


var table = new Tabulator("#example-table", {
	height: "90vh",
	layout: "fitColumns",
	columns: columnDefs,
	placeholder: "Awaiting Data, Please Load File",
	columnDefaults: {
		headerFilter: "input",
		resizable: true,
	},
    footerElement: '<span class="tabulator-counter float-left">'+
        'Showing <span id="search_count"></span> results out of <span id="total_count"></span> '+
        '</span>',	
});


table.on("dataLoaded", function(data){
    $("#total_count").text(data.length);
});

table.on("dataFiltered", function(filters, rows){
    $("#search_count").text(rows.length);
});

//trigger AJAX load on "Load Data via AJAX" button click
document.getElementById("file-load-trigger").addEventListener("click", function() {
	table.import("json", ".json");
});