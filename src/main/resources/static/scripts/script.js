//headerFilterFunc: (headerValue, rowValue, rowData, filterParams) => { return RegExp(headerValue, 'i').test(rowValue);}
var columnDefs = [{
    rowHandle: true,
    formatter: "handle",
    headerSort: false,
    frozen: true
  },
  {
    title: "ID",
    field: "ID",
    frozen: true
  },
  {
    title: "TYPE",
    field: "TYPE",
    frozen: true
  },
  {
    title: "INTERNAL-AUTH",
    field: "INTERNAL-AUTH"
  },
  {
    title: "EXTERNAL-AUTH",
    field: "EXTERNAL-AUTH"
  },
  {
    title: "ASSIGNED-IDP",
    field: "ASSIGNED-IDP"
  },
  {
    title: "SP-IDP",
    field: "SP-IDP"
  },
  {
    title: "COT",
    field: "COT",
  },
  {
    title: "ACCOUNT-MAPPER",
    field: "ACCOUNT-MAPPER"
  },
  {
    title: "ATTRIBUTE-MAPPER",
    field: "ATTRIBUTE-MAPPER"
  },
  {
    title: "HOSTED-REMOTE",
    field: "HOSTED-REMOTE"
  },
  {
    title: "REMARKS",
    field: "REMARKS",
    visible: false
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

$("#freetext-filter-value").keyup(function () {
  table.setFilter(matchAny, {
    value: $("#freetext-filter-value").val()
  });
  if ($("#freetext-filter-value").val() == " ") {
    table.clearFilter();
  }
});

//Custom filter example
function customFilter(data) {}

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

var table = new Tabulator("#openam-entities-table", {
  height:"75vh",
  layout: "fitDataStretch",
  responsiveLayout: "collapse",
  columns: columnDefs,
  placeholder: "Awaiting Data, Please Load File",
  groupBy: ["TYPE"],
  movableRows: true,
  rowClickPopup: function (e, row, onRendered) {
    return remarks(row.getData());
  },
  columnDefaults: {
    headerFilter: "input",
    resizable: true,
    tooltip: function (e, row, onRendered) {
      return remarks(row.getData());
    }
  },
  dataLoaderLoading: dataLoaderLoading,
  footerElement: '<span class="tabulator-counter float-left">' +
    'Showing <span id="search_count"></span> results out of <span id="total_count"></span> ' +
    '</span>',
  ajaxResponse: function (url, params, response) {
    //url - the URL of the request
    //params - the parameters passed with the request
    //response - the JSON object returned in the body of the response.
    var env = $("#selected-environment").val();
    dataTables[env] = response;
    return response; //return the response data to tabulator
  },
});


table.on("dataLoaded", function (data) {
  var env = $("#selected-environment").val();
  $("#total_count").text(data.length);
});

table.on("dataFiltered", function (filters, rows) {
  $("#search_count").text(rows.length);
});


$("#fetch-openam-test").click(function () {
  //table.setData("/openam/json/test");
});

$("#import-csv").click(function () {
  table.import("csv", ".csv");
});

$("#fetch-local").click(function () {
  var env = $("#selected-environment").val();
  table.setData("/rest/local/json?env=" + env);
});

$("#fetch-openam").click(function () {
  var env = $("#selected-environment").val();
  table.setData("/rest/openam/json?env=" + env);
});

$("#selected-environment").on('change', function() {
  var env = $("#selected-environment").val();
  console.log("--" + env);
  table.setData(dataTables[env]);
});

//Clear filters on "Clear Filters" button click
$("#filter-clear").click(function () {
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

$("#export-csv").click(function () {
  var env = $("#selected-environment").val();
  var filName = env + "-openam-" + timeStamp() + ".csv"
  table.download("csv", filName);
});

$("#all-apps-only").click(function () {
  table.clearFilter();
  table.setFilter([{
      field: "SP-IDP",
      type: "!=",
      value: "IDP"
    }, //filter by age greater than 52
    {
      field: "TYPE",
      type: "in",
      value: ["SAML2", "WSFED", "OAUTH2"]
    },
  ]);

});

$("#saml-apps-only").click(function () {
  table.clearFilter();
  table.setFilter([{
      field: "SP-IDP",
      type: "!=",
      value: "IDP"
    }, //filter by age greater than 52
    {
      field: "TYPE",
      type: "in",
      value: ["SAML2"]
    },
  ]);
});

$("#wsfed-apps-only").click(function () {
  table.clearFilter();
  table.setFilter([{
      field: "SP-IDP",
      type: "!=",
      value: "IDP"
    }, //filter by age greater than 52
    {
      field: "TYPE",
      type: "in",
      value: ["WSFED"]
    },
  ]);
});

$("#oauth-apps-only").click(function () {
  table.clearFilter();
  table.setFilter([{
      field: "TYPE",
      type: "in",
      value: ["OAUTH2"]
    },
  ]);
});



$("#2031-saml-wsfed-only").click(function () {
  table.clearFilter();
  table.setFilter([{
      field: "SP-IDP",
      type: "!=",
      value: "IDP"
    }, //filter by age greater than 52
    {
      field: "TYPE",
      type: "in",
      value: ["SAML2", "WSFED"]
    },
    {
      field: "COT",
      type: "regex",
      value: "31"
    },
  ]);
});

$("#2025-saml-wsfed-only").click(function () {
  table.clearFilter();
  table.setFilter([{
      field: "SP-IDP",
      type: "!=",
      value: "IDP"
    }, //filter by age greater than 52
    {
      field: "TYPE",
      type: "in",
      value: ["SAML2", "WSFED"]
    },
    {
      field: "COT",
      type: "regex",
      value: "^((?!31).)*$"
    },
  ]);
});


$("#internal-apps-only").click(function () {
  table.clearFilter();
  table.setFilter([{
      field: "SP-IDP",
      type: "!=",
      value: "IDP"
    }, //filter by age greater than 52
    {
      field: "TYPE",
      type: "in",
      value: ["SAML2", "WSFED", "OAUTH2"]
    },
    {
      field: "EXTERNAL-AUTH",
      type: "in",
      value: ["N/A"]
    },
  ]);
});

$("#saml-internal-only").click(function () {
  table.clearFilter();
  table.setFilter([{
      field: "SP-IDP",
      type: "!=",
      value: "IDP"
    }, //filter by age greater than 52
    {
      field: "TYPE",
      type: "in",
      value: ["SAML2"]
    },
    {
      field: "EXTERNAL-AUTH",
      type: "in",
      value: ["N/A"]
    },
  ]);
});

$("#wsfed-internal-only").click(function () {
  table.clearFilter();
  table.setFilter([{
      field: "SP-IDP",
      type: "!=",
      value: "IDP"
    }, //filter by age greater than 52
    {
      field: "TYPE",
      type: "in",
      value: ["WSFED"]
    },
    {
      field: "EXTERNAL-AUTH",
      type: "in",
      value: ["N/A"]
    },
  ]);
});

$("#oauth-internal-only").click(function () {
  table.clearFilter();
  table.setFilter([{
      field: "TYPE",
      type: "in",
      value: ["OAUTH2"]
    },
    {
      field: "EXTERNAL-AUTH",
      type: "in",
      value: ["N/A"]
    },
  ]);
});

$("#stats-only").click(function () {
  table.clearFilter();
  table.setFilter([{
      field: "TYPE",
      type: "in",
      value: ["STAT"]
    },
  ]);
});