const fs = require('fs');
const yargs = require('yargs');
const { Parser } = require('json2csv');

// Parse command-line arguments
const argv = yargs
  .option('i', {
    alias: 'input',
    description: 'Input file',
    type: 'string',
    demandOption: true
  })
  .option('o', {
    alias: 'output',
    description: 'Output file',
    type: 'string',
    demandOption: true
  })
  .option('filter', {
    description: 'Comma-delimited paths to filter by',
    type: 'string',
    demandOption: true
  })
  .help()
  .alias('help', 'h')
  .argv;

// Read input JSON data from file
const rawData = fs.readFileSync(argv.input);
const inputJson = JSON.parse(rawData);

// Function to filter and transform the input JSON
const transformJson = (input, filterPaths) => {
  const filterKeys = filterPaths.split(',').map(path => path.trim());
  const output = input.result.map(item => {
    const filteredObject = {};
    filterKeys.forEach(path => {
      const keys = path.split('.');
      let value = item;
      for (const key of keys) {
        if (value.hasOwnProperty(key)) {
          value = value[key];
        } else {
          value = undefined;
          break;
        }
      }
      filteredObject[keys[keys.length - 1]] = value;
    });
    return filteredObject;
  });

  return { result: output };
};

// Transform the input JSON based on the filter paths
const transformedJson = transformJson(inputJson, argv.filter);

// Write the transformed JSON to the output file
fs.writeFileSync(argv.output + '.json', JSON.stringify(transformedJson, null, 2));
console.log(`Transformed JSON file created: ${argv.output}.json`);

// Convert transformed data to CSV
const fields = Object.keys(transformedJson.result[0]);
const json2csvParser = new Parser({ fields });
const csv = json2csvParser.parse(transformedJson.result);

// Write the CSV to the output file
fs.writeFileSync(argv.output + '.csv', csv);
console.log(`Transformed CSV file created: ${argv.output}.csv`);
