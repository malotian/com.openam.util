var columnDefs = [
	{ headerName: "id", field: "id" },
	{ headerName: "type", field: "type" },
	{ headerName: "INTERNAL_AUTH_LEVEL", field: "INTERNAL_AUTH_LEVEL" },
	{ headerName: "ASSIGNED_IDENTITY_PROVIDER", field: "ASSIGNED_IDENTITY_PROVIDER" },
	{ headerName: "SERVICE_PROVIDER", field: "SERVICE_PROVIDER" },
	{ headerName: "IDENTITY_PROVIDER", field: "IDENTITY_PROVIDER" },
	{ headerName: "ASSIGNED_COT", field: "ASSIGNED_COT" },
	{ headerName: "EXTERNAL_AUTH_LEVEL", field: "EXTERNAL_AUTH_LEVEL" },
	{ headerName: "REMOTE", field: "REMOTE" },
	{ headerName: "HOSTED", field: "HOSTED" },

];

// specify the data
//var rowData = getRowData();// let the grid know which columns and what data to use

var gridOptions = {
	columnDefs: columnDefs,
	defaultColDef: {
		flex: 1,
		minWidth: 150,
		filter: true,
		sortable: true,
		floatingFilter: true,
	}
};

document.addEventListener('DOMContentLoaded', () => {
	var gridDiv = document.querySelector('#myGrid');
	new agGrid.Grid(gridDiv, gridOptions);

	fetch('/openam')
		.then((response) => response.json())
		.then((data) => gridOptions.api.setRowData(data));
});
