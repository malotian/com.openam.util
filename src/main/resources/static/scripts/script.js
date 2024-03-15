//define row context menu contents
var rowMenu = [
	{
		label: "<i class='fas fa-check-square'></i> Select Row",
		action: function(e, row) {
			row.select();
		}
	},
	{
		separator: true,
	},
	{
		label: "Admin Functions",
		menu: [
			{
				label: "<i class='fas fa-trash'></i> Delete Row",
				action: function(e, row) {
					row.delete();
				}
			},
			{
				label: "<i class='fas fa-ban'></i> Disabled Option",
				disabled: true,
			},
		]
	}
]

//define column header menu as column visibility toggle
var headerMenu = function() {
	var menu = [];
	var columns = this.getColumns();

	for (let column of columns) {

		if (true == column.getDefinition().rowHandle)
			continue;

		if (column.getDefinition().title.startsWith("-"))
			continue;

		//create checkbox element using font awesome icons
		let checkBox = document.createElement("input");
		checkBox.classList.add("form-check-input");
		checkBox.setAttribute("type", "checkbox");
		checkBox.checked = column.isVisible();

		//build label
		let label = document.createElement("span");
		let title = document.createElement("span");

		title.textContent = " " + column.getDefinition().title;


		label.appendChild(checkBox);
		label.appendChild(title);

		//create menu item
		menu.push({
			label: label,
			action: function(e) {
				//prevent menu closing
				e.stopPropagation();
				column.toggle();
				checkBox.checked = column.isVisible();
				table.redraw();
			}
		});
	}

	return menu;
};

//headerFilterFunc: (headerValue, rowValue, rowData, filterParams) => { return RegExp(headerValue, 'i').test(rowValue);}

var columnDefs = [{
	rowHandle: true,
	formatter: "handle",
	headerSort: false,
	frozen: true,
	width: 5,
	verAlign: "top",
	//formatter:"responsiveCollapse",

},
{
	title: "ID",
	field: "ID",
	frozen: true,
	widthGrow: 2,
},
{
	title: "TYPE",
	field: "TYPE",
	frozen: true,
	widthGrow: 0.7,
	mutator: function(value, data) {
		if (undefined === data["SP-IDP"])
			return data["TYPE"]
		return data["TYPE"] + "-" + data["SP-IDP"];
	}
},
{
	title: "INT",
	field: "INT",
	widthGrow: 0.7,
},
{
	title: "EXT",
	field: "EXT",
	widthGrow: 0.7,
},
{
	title: "IDP",
	field: "IDP",
	widthGrow: 1.5,
},
{
	title: "-TYPE2",
	field: "SP-IDP",
	visible: false
},
{
	title: "COT",
	field: "COT",
},
{
	title: "CLAIMS",
	field: "CLAIMS",
	widthGrow: 3,
	formatter: "textarea",
	mutator: function(value, data, type, params, component) {
		if (undefined == value)
			return "";
		const jsonArray = JSON.parse(value);
		const multilineString = jsonArray.join('\n');
		return multilineString;
	}
},
{
	title: "SIGNING-CERT-ALIAS",
	field: "SIGNING-CERT-ALIAS",
	widthGrow: 3,
	formatter: "textarea",
	mutator: function(value, data, type, params, component) {
		if (undefined == value)
			return "";
		const jsonArray = JSON.parse(value);
		const multilineString = jsonArray.join('\n');
		return multilineString;
	}
},
{
	title: "REDIRECT-URLS",
	field: "REDIRECT-URLS",
	widthGrow: 3,
	formatter: "textarea",
	mutator: function(value, data, type, params, component) {
		if (undefined == value)
			return "";
		const jsonArray = JSON.parse(value);
		const multilineString = jsonArray.join('\n');
		return multilineString;
	}
},
{
	title: "HOSTED-REMOTE",
	field: "HOSTED-REMOTE",
	widthGrow: 0.5,
},
{
	title: "REMARKS",
	field: "REMARKS",
	formatter: "textarea",
	widthGrow: 0.5,
	mutator: function(value, data, type, params, component) {
		if (undefined == value)
			return "";
		const jsonArray = JSON.parse(value);
		const multilineString = jsonArray.join('\n');
		return multilineString;
	}
}
];

var dataTables = {
	"stage": [],
	"prod": []
};

const template = document.createElement('template');
template.innerHTML = '<div style="display:inline-block;" class="d-flex flex-row">' +
	'<div>Be patient, Contacting OpenAM... </div>' +
	'<div class="ml-2 activity-sm" data-role="activity" data-type="atom" data-style="dark"></div>' +
	'</div>';
const dataLoaderLoading = template.content.firstChild;

var colFilterField = document.getElementById("col-filter-field");
var colFilterType = document.getElementById("col-filter-type");
var colFilterValue = document.getElementById("col-filter-value");
var freeTextFilterValue = document.getElementById("freetext-filter-value");

function matchAny(data, filterParams) {
	//data - the data for the row being filtered
	//filterParams - params object passed to the filter
	//RegExp - modifier "i" - case insensitve

	var match = false;
	const regex = RegExp(filterParams.value, 'i');

	for (var key in data) {
		if (regex.test(data[key]) == true) {
			match = true;
		}
	}
	return match;
}

$("#freetext-filter-value").keyup(function() {
	table.setFilter(matchAny, {
		value: $("#freetext-filter-value").val()
	});
	if ($("#freetext-filter-value").val() == " ") {
		table.clearFilter();
	}
});

//Custom filter example
function customFilter(data) { }

//Trigger setFilter function with correct parameters
function updateFilter() {
	var filterVal = colFilterField.options[colFilterField.selectedIndex].value;
	var typeVal = colFilterType.options[colFilterType.selectedIndex].value;

	var filter = filterVal == "function" ? customFilter : filterVal;

	if (filterVal == "function") {
		colFilterType.disabled = true;
		colFilterValue.disabled = true;
	} else {
		colFilterType.disabled = false;
		colFilterValue.disabled = false;
	}

	if (filterVal) {
		table.setFilter(filter, typeVal, colFilterValue.value);
	}
}

//Update filters on value change
document.getElementById("col-filter-field").addEventListener("change", updateFilter);
document.getElementById("col-filter-type").addEventListener("change", updateFilter);
document.getElementById("col-filter-value").addEventListener("keyup", updateFilter);

function clearFilterEx(row) {
	$('#quick-filter-none').prop("checked", true);
	colFilterField.value = "";
	colFilterType.value = "=";
	colFilterValue.value = "";
	freeTextFilterValue.value = "";
	table.clearFilter();
}

function pretty(value) {
	try {
		return JSON.stringify(value.split('\n'), false, 2).replace(/\\(['"])/g, '$1').replace(/['"]/g, '');
		// followng keeps nested quotes
		//		return JSON.stringify(jsonValue, false, 2).replace(/([^\\])"/g, '$1').replace(/\\(['"])/g, '$1');
	} catch (error) { }

	if (undefined === value || null === value)
		return "";

	return value;
}

function formatJSONTable(row) {

	let html = '<input id=\"copy-tool-tip-button\" type=\"button\" value=\"Copy\" style=\"float: right;\" onclick=\"copyCode(this);return false;\">';
	html += '<div id=\"tool-tip-contents\"><table>';
	var columns = table.getColumns();
	for (let column of columns) {

		if (true == column.getDefinition().rowHandle)
			continue;

		if (column.getDefinition().title.startsWith("-"))
			continue;

		let colTitle = column.getDefinition().title;
		let colValue = row[colTitle];
		if (colValue === undefined || colValue === null || colValue.trim().length === 0)
			colValue = "";
		else if (column.getDefinition().formatter === 'textarea')
			colValue = pretty(row[colTitle]);

		html += `<tr><pre style=\"display:inline\"><strong>${colTitle}:</strong>${colValue}</pre><br></tr>`;
	}
	html += '</table></div>';

	// Remove the header row
	const tableElement = document.createElement('div');
	tableElement.innerHTML = html;
	return tableElement.outerHTML;
}

async function copyCode(button) {
	button.value = "Copied";
	setTimeout(() => {
		button.value = "Copy";
	}, 700);
	
	var range = document.createRange();
	range.selectNode(document.getElementById("tool-tip-contents"));
	window.getSelection().removeAllRanges(); // clear current selection
	window.getSelection().addRange(range); // to select text
	document.execCommand("copy");
	window.getSelection().removeAllRanges();// to deselect
}


function remarks(row) {
	container = document.createElement("div");
	container.innerHTML = formatJSONTable(row);
	return container;
}

var table = new Tabulator("#openam-entities-table", {
	height: "75vh",
	layout: "fitColumns",
	responsiveLayout: "collapse",
	//rowContextMenu: rowMenu, //add context menu to rows
	columns: columnDefs,
	placeholder: "Awaiting Data, Please Load File",
	groupBy: ["TYPE"],
	movableRows: true,
	//rowClickPopup: function(e, row, onRendered) {
	//return remarks(row.getData());
	//},
	columnDefaults: {
		headerFilter: "input",
		resizable: true,
		headerMenu: headerMenu,
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
		var env = $("#selected-environment").val();
		dataTables[env] = response;
		return response; //return the response data to tabulator
	},
});

table.on("cellClick", function(e, cell) {
	cell.popup(remarks(cell.getRow().getData()), "center");
});

table.on("dataLoaded", function(data) {
	var env = $("#selected-environment").val();
	$("#total_count").text(data.length);
	table.redraw();
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
	var env = $("#selected-environment").val();
	table.setData("/rest/local/json?env=" + env);
});

$("#fetch-openam").click(function() {
	var env = $("#selected-environment").val();
	table.setData("/rest/openam/json?env=" + env);
});

// A $( document ).ready() block.
table.on("tableBuilding", function() {
	$.ajax({
		url: "/rest/table/column/visible", success: function(result) {
			table.getColumns(true).forEach((col) => {
				if (undefined !== col.getDefinition().field) {
					if (false === result.includes(col.getDefinition().field)) {
						col.hide();
						console.log("hiding: " + col.getDefinition().field);
					}
				}
			});
		}
	});
});

table.on("dataProcessed", function() {
	table.redraw(true);
});

$("#selected-environment").on('change', function() {
	var env = $("#selected-environment").val();
	console.log("--" + env);
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

$("#export-pdf").click(function() {
	var env = $("#selected-environment").val();
	var filName = env + "-openam-" + timeStamp() + ".pdf";
	table.download("pdf", filName, {
		orientation: "landscape", //set page orientation to portrait
		title: env, //add title to report
		jsPDF: {
			format: "b3"
		},
	});
});


$("#export-csv").click(function() {
	var env = $("#selected-environment").val();
	var filName = env + "-openam-" + timeStamp() + ".csv"
	table.download("csv", filName);
});

$("#all-apps-only").click(function() {
	table.clearFilter();
	table.setFilter([{
		field: "SP-IDP",
		type: "!=",
		value: "IDP"
	}, //filter by age greater than 52
	{
		field: "TYPE",
		type: "in",
		value: ["SAML2-SP", "WSFED-SP", "OAUTH2"]
	},
	]);

});

$("#saml-apps-only").click(function() {
	table.clearFilter();
	table.setFilter([{
		field: "SP-IDP",
		type: "!=",
		value: "IDP"
	}, //filter by age greater than 52
	{
		field: "TYPE",
		type: "in",
		value: ["SAML2-SP"]
	},
	]);
});

$("#wsfed-apps-only").click(function() {
	table.clearFilter();
	table.setFilter([{
		field: "SP-IDP",
		type: "!=",
		value: "IDP"
	}, //filter by age greater than 52
	{
		field: "TYPE",
		type: "in",
		value: ["WSFED-SP"]
	},
	]);
});

$("#oauth-apps-only").click(function() {
	table.clearFilter();
	table.setFilter([{
		field: "TYPE",
		type: "in",
		value: ["OAUTH2"]
	},
	]);
});



$("#2031-saml-wsfed-only").click(function() {
	table.clearFilter();
	table.setFilter([{
		field: "SP-IDP",
		type: "!=",
		value: "IDP"
	}, //filter by age greater than 52
	{
		field: "TYPE",
		type: "in",
		value: ["SAML2-SP", "WSFED-SP"]
	},
	{
		field: "COT",
		type: "regex",
		value: "31"
	},
	]);
});

$("#2025-saml-wsfed-only").click(function() {
	table.clearFilter();
	table.setFilter([{
		field: "SP-IDP",
		type: "!=",
		value: "IDP"
	}, //filter by age greater than 52
	{
		field: "TYPE",
		type: "in",
		value: ["SAML2-SP", "WSFED-SP"]
	},
	{
		field: "COT",
		type: "regex",
		value: "^((?!31).)*$"
	},
	]);
});


$("#internal-apps-only").click(function() {
	table.clearFilter();
	table.setFilter([{
		field: "SP-IDP",
		type: "!=",
		value: "IDP"
	},
	{
		field: "TYPE",
		type: "in",
		value: ["SAML2-SP", "WSFED-SP", "OAUTH2"]
	},
	{
		field: "EXT",
		type: "in",
		value: ["N/A"]
	},
	]);
});

$("#saml-internal-only").click(function() {
	table.clearFilter();
	table.setFilter([{
		field: "SP-IDP",
		type: "!=",
		value: "IDP"
	}, //filter by age greater than 52
	{
		field: "TYPE",
		type: "in",
		value: ["SAML2-SP"]
	},
	{
		field: "EXT",
		type: "in",
		value: ["N/A"]
	},
	]);
});

$("#wsfed-internal-only").click(function() {
	table.clearFilter();
	table.setFilter([{
		field: "SP-IDP",
		type: "!=",
		value: "IDP"
	}, //filter by age greater than 52
	{
		field: "TYPE",
		type: "in",
		value: ["WSFED-SP"]
	},
	{
		field: "EXT",
		type: "in",
		value: ["N/A"]
	},
	]);
});

$("#oauth-internal-only").click(function() {
	table.clearFilter();
	table.setFilter([{
		field: "TYPE",
		type: "in",
		value: ["OAUTH2"]
	},
	{
		field: "EXT",
		type: "in",
		value: ["N/A"]
	},
	]);
});

$("#stats-only").click(function() {
	table.clearFilter();
	table.setFilter([{
		field: "TYPE",
		type: "in",
		value: ["STAT"]
	},
	]);
});