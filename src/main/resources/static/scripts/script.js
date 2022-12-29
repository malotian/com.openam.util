//headerFilterFunc: (headerValue, rowValue, rowData, filterParams) => { return RegExp(headerValue, 'i').test(rowValue);}
var columnDefs = [
	{ title: "ID", field: "ID", frozen: true },
	{ title: "TYPE", field: "TYPE", frozen: true },
	{ title: "INTERNAL-AUTH", field: "INTERNAL-AUTH" },
	{ title: "EXTERNAL-AUTH", field: "EXTERNAL-AUTH" },
	{ title: "ASSIGNED-IDP", field: "ASSIGNED-IDP" },
	{ title: "SP-IDP", field: "SP-IDP" },
	{ title: "COT", field: "COT", },
	{ title: "HOSTED-REMOTE", field: "HOSTED-REMOTE" },
	{ title: "REMARKS", field: "REMARKS", visible: false }
];

const template = document.createElement('template');
template.innerHTML = '<div style="display:inline-block;" class="d-flex flex-row">' +
	'<div>Be patient, Contacting OpenAM... </div>' +
	'<div class="ml-2 activity-sm" data-role="activity" data-type="atom" data-style="dark"></div>' +
	'</div>';
const dataLoaderLoading = template.content.firstChild;

var fieldEl = document.getElementById("filter-field");
var typeEl = document.getElementById("filter-type");
var valueEl = document.getElementById("filter-value");

//Custom filter example
function customFilter(data) {
	return data.car && data.rating < 3;
}

//Trigger setFilter function with correct parameters
function updateFilter() {
	var filterVal = fieldEl.options[fieldEl.selectedIndex].value;
	var typeVal = typeEl.options[typeEl.selectedIndex].value;

	var filter = filterVal == "function" ? customFilter : filterVal;

	if (filterVal == "function") {
		typeEl.disabled = true;
		valueEl.disabled = true;
	} else {
		typeEl.disabled = false;
		valueEl.disabled = false;
	}

	if (filterVal) {
		table.setFilter(filter, typeVal, valueEl.value);
	}
}

//Update filters on value change
document.getElementById("filter-field").addEventListener("change", updateFilter);
document.getElementById("filter-type").addEventListener("change", updateFilter);
document.getElementById("filter-value").addEventListener("keyup", updateFilter);

//Clear filters on "Clear Filters" button click
document.getElementById("filter-clear").addEventListener("click", function() {
	fieldEl.value = "";
	typeEl.value = "=";
	valueEl.value = "";

	table.clearFilter();
});

function remarks(row) {
	container = document.createElement("div");
	contents = "<strong style='font-size:1.2em;'>" + row["ID"] + "</strong><br/><ul style='padding:0;  margin-top:10px; margin-bottom:0;'>";
	const items = row["REMARKS"].split("#");
	for (const item of items) {
		const items = row["REMARKS"].split("#");

		contents += "<li>" + item + "</li>";
	}
	contents += "</ul>";
	container.innerHTML = contents;

	return container;
}


var table = new Tabulator("#example-table", {
	height: "85vh",
	layout: "fitDataStretch",
	responsiveLayout: "collapse",
	columns: columnDefs,
	placeholder: "Awaiting Data, Please Load File",
	groupBy: ["TYPE"],
	rowClickPopup: function(e, row, onRendered) {
		return remarks(row.getData());
	},
	columnDefaults: {
		headerFilter: "input",
		resizable: true,
		tooltip: function(e, row, onRendered) {
			return remarks(row.getData());
		}
	},
	dataLoaderLoading: dataLoaderLoading,
	footerElement: '<span class="tabulator-counter float-left">' +
		'Showing <span id="search_count"></span> results out of <span id="total_count"></span> ' +
		'</span>',
});


table.on("dataLoaded", function(data) {
	$("#total_count").text(data.length);
});

table.on("dataFiltered", function(filters, rows) {
	$("#search_count").text(rows.length);
});

document.getElementById("import-csv").addEventListener("click", function() {
	table.import("csv", ".csv");
});

document.getElementById("fetch-openam").addEventListener("click", function() {
	table.setData("/openam/json");
});

document.getElementById("export-csv").addEventListener("click", function() {
	table.download("csv", "openam.csv");
});