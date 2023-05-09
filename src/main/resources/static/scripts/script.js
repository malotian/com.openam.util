//headerFilterFunc: (headerValue, rowValue, rowData, filterParams) => { return RegExp(headerValue, 'i').test(rowValue);}
var columnDefs = [
	{ rowHandle: true, formatter: "handle", headerSort: false, frozen: true },
	{ title: "ID", field: "ID", frozen: true },
	{ title: "TYPE", field: "TYPE", frozen: true },
	{ title: "INTERNAL-AUTH", field: "INTERNAL-AUTH" },
	{ title: "EXTERNAL-AUTH", field: "EXTERNAL-AUTH" },
	{ title: "ASSIGNED-IDP", field: "ASSIGNED-IDP" },
	{ title: "SP-IDP", field: "SP-IDP" },
	{ title: "COT", field: "COT", },
	{ title: "ACCOUNT-MAPPER", field: "ACCOUNT-MAPPER" },
	{ title: "ATTRIBUTE-MAPPER", field: "ATTRIBUTE-MAPPER" },
	{ title: "HOSTED-REMOTE", field: "HOSTED-REMOTE" },
	{ title: "REMARKS", field: "REMARKS", visible: false }
];

var dataTables = { "stage": [], "prod": [] };

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

function clearFilterEx(row) {
	fieldEl.value = "";
	typeEl.value = "=";
	valueEl.value = "";
	table.clearFilter();
}


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
	movableRows: true,
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
	ajaxResponse: function(url, params, response) {
		//url - the URL of the request
		//params - the parameters passed with the request
		//response - the JSON object returned in the body of the response.
		var env = $('input:radio[name=env]:checked').val();
		dataTables[env] = response;
		return response; //return the response data to tabulator
	},
});


table.on("dataLoaded", function(data) {
	var env = $('input:radio[name=env]:checked').val();
	$("#total_count").text(data.length);
});

table.on("dataFiltered", function(filters, rows) {
	$("#search_count").text(rows.length);
});


$("#fetch-openam-test").click(function() {
	//table.setData("/openam/json/test");
});

$("#import-csv").click(function() {
	table.import("csv", ".csv");
});

$("#fetch-local").click(function() {
	var env = $('input:radio[name=env]:checked').val();
	table.setData("/rest/local/json?env=" + env);
});

$("#fetch-openam").click(function() {
	var env = $('input:radio[name=env]:checked').val();
	table.setData("/rest/openam/json?env=" + env);
});

$("input:radio[name='env']").change(function() {
	var env = $('input:radio[name=env]:checked').val();
	table.setData(dataTables[env]);
});

//Clear filters on "Clear Filters" button click
$("#filter-clear").click(function() {
	clearFilterEx();
});


function pad(n) {
	return n < 10 ? '0' + n : n;
}

function timeStamp() {
	const now = new Date();
	const localDateTime = now.getFullYear() + "-" + pad(now.getMonth() + 1) + "-" + pad(now.getDate()) + "-" +
		pad(now.getHours()) + "-" + pad(now.getMinutes()) + "-" + pad(now.getSeconds());
	return localDateTime;
}

$("#export-csv").click(function() {
	var env = $('input:radio[name=env]:checked').val();
	var filName = env + "-openam-"+ timeStamp() + ".csv"
	table.download("csv", filName);
});

$("#all-apps-only").click(function() {
	table.clearFilter();
	table.setFilter
		([
			{ field: "SP-IDP", type: "!=", value: "IDP" }, //filter by age greater than 52
			{ field: "TYPE", type: "in", value: ["SAML2", "WSFED", "OAUTH2"] }, //name must be steve, bob or jim
		]);

});

$("#saml-apps-only").click(function() {
	table.clearFilter();
	table.setFilter
		([
			{ field: "SP-IDP", type: "!=", value: "IDP" }, //filter by age greater than 52
			{ field: "TYPE", type: "in", value: ["SAML2"] }, //name must be steve, bob or jim
		]);
});

$("#wsfed-apps-only").click(function() {
	table.clearFilter();
	table.setFilter
		([
			{ field: "SP-IDP", type: "!=", value: "IDP" }, //filter by age greater than 52
			{ field: "TYPE", type: "in", value: ["WSFED"] }, //name must be steve, bob or jim
		]);
});

$("#oauth-apps-only").click(function() {
	table.clearFilter();
	table.setFilter
		([
			{ field: "TYPE", type: "in", value: ["OAUTH2"] }, //name must be steve, bob or jim
		]);
});



$("#2031-saml-wsfed-only").click(function() {
	table.clearFilter();
	table.setFilter
		([
			{ field: "SP-IDP", type: "!=", value: "IDP" }, //filter by age greater than 52
			{ field: "TYPE", type: "in", value: ["SAML2", "WSFED"] }, //name must be steve, bob or jim
			{ field: "COT", type: "regex", value: "31" }, //name must be steve, bob or jim
		]);
});

$("#2025-saml-wsfed-only").click(function() {
	table.clearFilter();
	table.setFilter
		([
			{ field: "SP-IDP", type: "!=", value: "IDP" }, //filter by age greater than 52
			{ field: "TYPE", type: "in", value: ["SAML2", "WSFED"] }, //name must be steve, bob or jim
			{ field: "COT", type: "regex", value: "^((?!31).)*$" }, //name must be steve, bob or jim
		]);
});


$("#internal-apps-only").click(function() {
	table.clearFilter();
	table.setFilter
		([
			{ field: "SP-IDP", type: "!=", value: "IDP" }, //filter by age greater than 52
			{ field: "TYPE", type: "in", value: ["SAML2", "WSFED", "OAUTH2"] }, //name must be steve, bob or jim
			{ field: "EXTERNAL-AUTH", type: "in", value: ["N/A"] }, //name must be steve, bob or jim
		]);
});

$("#saml-internal-only").click(function() {
	table.clearFilter();
	table.setFilter
		([
			{ field: "SP-IDP", type: "!=", value: "IDP" }, //filter by age greater than 52
			{ field: "TYPE", type: "in", value: ["SAML2"] }, //name must be steve, bob or jim
			{ field: "EXTERNAL-AUTH", type: "in", value: ["N/A"] }, //name must be steve, bob or jim
		]);
});

$("#wsfed-internal-only").click(function() {
	table.clearFilter();
	table.setFilter
		([
			{ field: "SP-IDP", type: "!=", value: "IDP" }, //filter by age greater than 52
			{ field: "TYPE", type: "in", value: ["WSFED"] }, //name must be steve, bob or jim
			{ field: "EXTERNAL-AUTH", type: "in", value: ["N/A"] }, //name must be steve, bob or jim
		]);
});

$("#oauth-internal-only").click(function() {
	table.clearFilter();
	table.setFilter
		([
			{ field: "TYPE", type: "in", value: ["OAUTH2"] }, //name must be steve, bob or jim
			{ field: "EXTERNAL-AUTH", type: "in", value: ["N/A"] }, //name must be steve, bob or jim
		]);
});

$("#stats-only").click(function() {
	table.clearFilter();
	table.setFilter
		([
			{ field: "TYPE", type: "in", value: ["STAT"] }, //name must be steve, bob or jim
		]);
});
