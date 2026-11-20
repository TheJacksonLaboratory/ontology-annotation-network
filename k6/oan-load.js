// k6 load test for the oan-rest API.
//
// Replays the production endpoint mix (Cloud Run request logs, 24h sample):
//   annotation/{id} 47%, search/gene 26%, search/disease 26%,
//   search/disease/intersect 0.5%, annotation/{id}/download/gene 0.5%.
// The logs collapse IDs, so the annotation split (2% broad HP terms, then roughly
// equal phenotype, gene, and disease) is an estimate. Latency thresholds are
// provisional starting points.
//
// Usage:
//   k6 run -e BASE_URL=https://<lb-host>/api/network [-e PROFILE=load] k6/oan-load.js
//   Add -e LOG_FAILURES=<n> to change how many non-200 responses each VU logs (default 20).
//
// Profiles (constant or ramping arrival rate, in requests per second). Cloud Armor
// throttles each client IP to 300 requests per 30s (10 rps) and answers 429 above
// that, so no profile exceeds 9 rps:
//   smoke   2 rps for 1m                                  sanity check before anything else
//   load    ramp to 9 rps, hold 10m                        near the prod 1-min peak (11 rps)
//   stress  3 -> 6 -> 9 rps, 3m per step                   latency as load rises to the cap
//   spike   1 rps, then 9 rps for 2m                       sudden jump on the warm instance
//   soak    5 rps for 60m                                  memory and connection leaks
//
// Every ID and search term below returns data, so a 200 response with an empty body
// counts as a failure (metric "empty_body"). The repositories log query errors and
// return empty results instead of an error status.

import http from 'k6/http';
import { check } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || '').replace(/\/+$/, '');
if (!BASE_URL) {
  throw new Error('Set BASE_URL, e.g. -e BASE_URL=https://<lb-host>/api/network');
}
if (!/^https?:\/\/[^/]+/.test(BASE_URL)) {
  throw new Error(`BASE_URL "${BASE_URL}" must start with http:// or https://`);
}
const PROFILE = __ENV.PROFILE || 'smoke';

const emptyBody = new Rate('empty_body');

const MAX_LOGGED_FAILURES = Number(__ENV.LOG_FAILURES || 20);
let loggedFailures = 0;

const PHENOTYPES = [
  'HP:0000030', 'HP:0000222', 'HP:0000402', 'HP:0000403', 'HP:0000591', 'HP:0000851',
  'HP:0000939', 'HP:0001010', 'HP:0001308', 'HP:0001341', 'HP:0001677', 'HP:0001880',
  'HP:0002121', 'HP:0002476', 'HP:0002886', 'HP:0002943', 'HP:0003105', 'HP:0003112',
  'HP:0003304', 'HP:0003565', 'HP:0003596', 'HP:0003764', 'HP:0003957', 'HP:0004027',
  'HP:0004241', 'HP:0004306', 'HP:0004565', 'HP:0004722', 'HP:0004826', 'HP:0004828',
  'HP:0005004', 'HP:0005900', 'HP:0006161', 'HP:0006467', 'HP:0006615', 'HP:0006731',
  'HP:0006915', 'HP:0006976', 'HP:0007069', 'HP:0007230', 'HP:0007610', 'HP:0007841',
  'HP:0007947', 'HP:0008161', 'HP:0008223', 'HP:0008392', 'HP:0008480', 'HP:0008762',
  'HP:0009105', 'HP:0009775', 'HP:0009907', 'HP:0009909', 'HP:0009939', 'HP:0010001',
  'HP:0010287', 'HP:0010381', 'HP:0010658', 'HP:0010747', 'HP:0010838', 'HP:0011057',
  'HP:0011223', 'HP:0011307', 'HP:0011334', 'HP:0011729', 'HP:0011794', 'HP:0011872',
  'HP:0011894', 'HP:0011959', 'HP:0012019', 'HP:0012347', 'HP:0012402', 'HP:0012802',
  'HP:0025430', 'HP:0025463', 'HP:0025480', 'HP:0025815', 'HP:0030019', 'HP:0030409',
  'HP:0030499', 'HP:0030889', 'HP:0031291', 'HP:0031398', 'HP:0032051', 'HP:0032124',
  'HP:0032139', 'HP:0033842', 'HP:0033996', 'HP:0034284', 'HP:0034893', 'HP:0040146',
  'HP:0040303', 'HP:0100279', 'HP:0100544', 'HP:0100739', 'HP:0200016', 'HP:0410280',
  'HP:0430000', 'HP:0430023', 'HP:0500034', 'HP:0500049', 'HP:4000207', 'HP:5210195',
  'HP:6000826', 'HP:6000916', 'HP:6001034',
];
const GENES = [
  'NCBIGene:21', 'NCBIGene:155', 'NCBIGene:181', 'NCBIGene:204', 'NCBIGene:226', 'NCBIGene:523',
  'NCBIGene:667', 'NCBIGene:668', 'NCBIGene:745', 'NCBIGene:775', 'NCBIGene:781', 'NCBIGene:783',
  'NCBIGene:843', 'NCBIGene:845', 'NCBIGene:857', 'NCBIGene:1001', 'NCBIGene:1436', 'NCBIGene:1438',
  'NCBIGene:1482', 'NCBIGene:1490', 'NCBIGene:1496', 'NCBIGene:1804', 'NCBIGene:1813', 'NCBIGene:1861',
  'NCBIGene:2153', 'NCBIGene:2186', 'NCBIGene:2261', 'NCBIGene:2263', 'NCBIGene:2301', 'NCBIGene:2312',
  'NCBIGene:2316', 'NCBIGene:2706', 'NCBIGene:2736', 'NCBIGene:2778', 'NCBIGene:2908', 'NCBIGene:2990',
  'NCBIGene:3106', 'NCBIGene:3172', 'NCBIGene:3209', 'NCBIGene:3291', 'NCBIGene:3425', 'NCBIGene:3459',
  'NCBIGene:3561', 'NCBIGene:3730', 'NCBIGene:3752', 'NCBIGene:3764', 'NCBIGene:3766', 'NCBIGene:3767',
  'NCBIGene:3769', 'NCBIGene:3779', 'NCBIGene:3849', 'NCBIGene:3897', 'NCBIGene:4000', 'NCBIGene:4001',
  'NCBIGene:4247', 'NCBIGene:4524', 'NCBIGene:4763', 'NCBIGene:4905', 'NCBIGene:4921', 'NCBIGene:5015',
  'NCBIGene:5167', 'NCBIGene:5318', 'NCBIGene:5361', 'NCBIGene:5443', 'NCBIGene:5468', 'NCBIGene:5631',
  'NCBIGene:5718', 'NCBIGene:5727', 'NCBIGene:5826', 'NCBIGene:5860', 'NCBIGene:5961', 'NCBIGene:6121',
  'NCBIGene:6137', 'NCBIGene:6204', 'NCBIGene:6297', 'NCBIGene:6324', 'NCBIGene:6327', 'NCBIGene:6331',
  'NCBIGene:6336', 'NCBIGene:6337', 'NCBIGene:6338', 'NCBIGene:6499', 'NCBIGene:6513', 'NCBIGene:6569',
  'NCBIGene:6650', 'NCBIGene:6658', 'NCBIGene:6697', 'NCBIGene:6833', 'NCBIGene:6905', 'NCBIGene:7352',
  'NCBIGene:7471', 'NCBIGene:7480', 'NCBIGene:7515', 'NCBIGene:7555', 'NCBIGene:7840', 'NCBIGene:7871',
  'NCBIGene:8106', 'NCBIGene:8295', 'NCBIGene:8431', 'NCBIGene:8438', 'NCBIGene:8642', 'NCBIGene:8675',
  'NCBIGene:8910', 'NCBIGene:8991', 'NCBIGene:9200', 'NCBIGene:9227', 'NCBIGene:9241', 'NCBIGene:9382',
  'NCBIGene:9423', 'NCBIGene:9607', 'NCBIGene:9672', 'NCBIGene:9681', 'NCBIGene:9742', 'NCBIGene:10008',
  'NCBIGene:10021', 'NCBIGene:10046', 'NCBIGene:10060', 'NCBIGene:10142', 'NCBIGene:10352', 'NCBIGene:10371',
  'NCBIGene:10999', 'NCBIGene:22807', 'NCBIGene:22856', 'NCBIGene:22943', 'NCBIGene:23005', 'NCBIGene:23171',
  'NCBIGene:23259', 'NCBIGene:23279', 'NCBIGene:23347', 'NCBIGene:23426', 'NCBIGene:23556', 'NCBIGene:23630',
  'NCBIGene:25942', 'NCBIGene:26040', 'NCBIGene:29098', 'NCBIGene:29954', 'NCBIGene:50814', 'NCBIGene:51738',
  'NCBIGene:51778', 'NCBIGene:54345', 'NCBIGene:54795', 'NCBIGene:55210', 'NCBIGene:55630', 'NCBIGene:55681',
  'NCBIGene:55800', 'NCBIGene:55812', 'NCBIGene:56006', 'NCBIGene:57003', 'NCBIGene:57010', 'NCBIGene:57216',
];
const OMIM = [
  'OMIM:118420', 'OMIM:137100', 'OMIM:137940', 'OMIM:140850', 'OMIM:141350', 'OMIM:146800',
  'OMIM:149730', 'OMIM:168860', 'OMIM:176305', 'OMIM:184460', 'OMIM:185460', 'OMIM:201100',
  'OMIM:216820', 'OMIM:219730', 'OMIM:221820', 'OMIM:226960', 'OMIM:248910', 'OMIM:249240',
  'OMIM:249420', 'OMIM:253220', 'OMIM:259410', 'OMIM:267500', 'OMIM:269150', 'OMIM:271665',
  'OMIM:277170', 'OMIM:300661', 'OMIM:300758', 'OMIM:300770', 'OMIM:300831',
  'OMIM:300858', 'OMIM:312000', 'OMIM:598500', 'OMIM:601544', 'OMIM:601665', 'OMIM:602668',
  'OMIM:603233', 'OMIM:603909', 'OMIM:605282', 'OMIM:606721', 'OMIM:608622', 'OMIM:610829',
  'OMIM:610921', 'OMIM:611209', 'OMIM:611881', 'OMIM:611938', 'OMIM:612286', 'OMIM:613156',
  'OMIM:613308', 'OMIM:613710', 'OMIM:613807', 'OMIM:613838', 'OMIM:614149', 'OMIM:614298',
  'OMIM:614432', 'OMIM:614435', 'OMIM:614602', 'OMIM:614927', 'OMIM:615425', 'OMIM:615846',
  'OMIM:615978', 'OMIM:616026', 'OMIM:616063', 'OMIM:616516', 'OMIM:616828', 'OMIM:617271',
  'OMIM:617667', 'OMIM:617781', 'OMIM:618090', 'OMIM:618113', 'OMIM:618148', 'OMIM:618174',
  'OMIM:618178', 'OMIM:618264', 'OMIM:618268', 'OMIM:618728', 'OMIM:618778', 'OMIM:618939',
  'OMIM:619053', 'OMIM:619125', 'OMIM:619318', 'OMIM:619340', 'OMIM:619402', 'OMIM:619501',
  'OMIM:619702', 'OMIM:619955', 'OMIM:619967', 'OMIM:619995', 'OMIM:620250', 'OMIM:620504',
  'OMIM:621150', 'OMIM:621196', 'OMIM:621234',
];
const ORPHA = [
  'ORPHA:100073', 'ORPHA:101007', 'ORPHA:1143', 'ORPHA:1296', 'ORPHA:130', 'ORPHA:139474',
  'ORPHA:1450', 'ORPHA:158061', 'ORPHA:1711', 'ORPHA:171629', 'ORPHA:1875', 'ORPHA:188',
  'ORPHA:199343', 'ORPHA:2115', 'ORPHA:2250', 'ORPHA:231736', 'ORPHA:251076', 'ORPHA:2514',
  'ORPHA:2703', 'ORPHA:276', 'ORPHA:276435', 'ORPHA:276580', 'ORPHA:2786', 'ORPHA:2819',
  'ORPHA:284271', 'ORPHA:29207', 'ORPHA:3052', 'ORPHA:306617', 'ORPHA:314679', 'ORPHA:320',
  'ORPHA:326', 'ORPHA:3379', 'ORPHA:364055', 'ORPHA:36899', 'ORPHA:464318', 'ORPHA:496756',
  'ORPHA:496790', 'ORPHA:529962', 'ORPHA:563609', 'ORPHA:572333', 'ORPHA:576', 'ORPHA:70594',
  'ORPHA:77301', 'ORPHA:786', 'ORPHA:79314', 'ORPHA:79329', 'ORPHA:83453', 'ORPHA:83461',
  'ORPHA:85193', 'ORPHA:88621', 'ORPHA:90652', 'ORPHA:93274', 'ORPHA:93476', 'ORPHA:94065',
  'ORPHA:95455', 'ORPHA:98811', 'ORPHA:98856',
];
const DISEASES = OMIM.concat(ORPHA);

// High-level HP terms with large responses (0.6-1.7 MB; HP:0000118 takes ~1.6s).
const BROAD_PHENOTYPES = [
  'HP:0000118', 'HP:0000707', 'HP:0000478', 'HP:0000152', 'HP:0000924',
  'HP:0001626', 'HP:0001939', 'HP:0001574', 'HP:0000119', 'HP:0001507',
];

const GENE_TERMS = ['apo', 'brc', 'col', 'fbn', 'kcn', 'myo', 'sca', 'tp5', 'car', 'epi', 'ata', 'dys', 'syn', 'neu', 'mus', 'ret', 'def', 'cer'];
const DISEASE_TERMS = ['apo', 'col', 'kcn', 'myo', 'sca', 'car', 'epi', 'ata', 'dys', 'syn', 'neu', 'mus', 'ret', 'def', 'cer'];

const ARRIVAL = { preAllocatedVUs: 50, maxVUs: 400, timeUnit: '1s' };

const PROFILES = {
  smoke: { executor: 'constant-arrival-rate', rate: 2, duration: '1m', ...ARRIVAL },
  load: {
    executor: 'ramping-arrival-rate', startRate: 1, ...ARRIVAL,
    stages: [
      { target: 9, duration: '2m' },
      { target: 9, duration: '10m' },
      { target: 0, duration: '1m' },
    ],
  },
  stress: {
    executor: 'ramping-arrival-rate', startRate: 1, ...ARRIVAL,
    stages: [
      { target: 3, duration: '1m' }, { target: 3, duration: '3m' },
      { target: 6, duration: '1m' }, { target: 6, duration: '3m' },
      { target: 9, duration: '1m' }, { target: 9, duration: '3m' },
      { target: 0, duration: '1m' },
    ],
  },
  spike: {
    executor: 'ramping-arrival-rate', startRate: 1, ...ARRIVAL,
    stages: [
      { target: 1, duration: '1m' },
      { target: 9, duration: '10s' },
      { target: 9, duration: '2m' },
      { target: 1, duration: '10s' },
      { target: 1, duration: '1m' },
    ],
  },
  soak: { executor: 'constant-arrival-rate', rate: 5, duration: '60m', ...ARRIVAL },
};

if (!PROFILES[PROFILE]) {
  throw new Error(`Unknown PROFILE "${PROFILE}"; use one of ${Object.keys(PROFILES).join(', ')}`);
}

export const options = {
  scenarios: { [PROFILE]: PROFILES[PROFILE] },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    empty_body: ['rate<0.001'],
    'http_req_duration{endpoint:search}': ['p(95)<300', 'p(99)<1000'],
    'http_req_duration{endpoint:annotation}': ['p(95)<1000', 'p(99)<3000'],
    'http_req_duration{endpoint:annotation-broad}': ['p(95)<4000'],
    dropped_iterations: ['count<10'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

/**
 * Returns a random element of an array.
 *
 * @param {Array<T>} items non-empty array to pick from
 * @returns {T} one element of items
 * @template T
 */
function pick(items) {
  return items[Math.floor(Math.random() * items.length)];
}

/**
 * Returns the response body parsed as JSON, or null if it is not valid JSON.
 *
 * @param {import('k6/http').RefinedResponse} res the HTTP response
 * @returns {?Object} the parsed body, or null
 */
function json(res) {
  try {
    return res.json();
  } catch (e) {
    return null;
  }
}

/**
 * Returns the length of a JSON array field, treating a missing field as empty.
 *
 * @param {?Array} value the array field
 * @returns {number} the array length, or 0
 */
function size(value) {
  return Array.isArray(value) ? value.length : 0;
}

/**
 * Sends a GET request, checks for a 200 status and a non-empty body, and records
 * the result in the empty_body metric. Logs the first LOG_FAILURES (default 20)
 * non-200 responses in each VU.
 *
 * @param {string} path path under BASE_URL, starting with a slash
 * @param {Object<string, string>} tags request tags; "name" groups URLs in the summary
 * @param {?function(Object): boolean} hasData returns true if the parsed body holds
 *     results; null skips the emptiness check
 */
function get(path, tags, hasData) {
  const res = http.get(`${BASE_URL}${path}`, { tags });
  if (res.status !== 200 && loggedFailures < MAX_LOGGED_FAILURES) {
    loggedFailures++;
    const body = String(res.body || res.error || '').slice(0, 200).replace(/\s+/g, ' ');
    console.warn(`${res.status} ${path} server=${res.headers['Server'] || '-'} via=${res.headers['Via'] || '-'} body=${body}`);
  }
  const checks = { 'status is 200': (r) => r.status === 200 };
  if (hasData) {
    const ok = res.status === 200 && hasData(json(res) || {});
    emptyBody.add(!ok, tags);
    checks['body has data'] = () => ok;
  }
  check(res, checks, tags);
}

/**
 * Sends one request, chosen to match the production endpoint mix.
 */
export default function () {
  const r = Math.random();
  if (r < 0.47) {
    const kind = Math.random();
    if (kind < 0.02) {
      get(`/annotation/${pick(BROAD_PHENOTYPES)}`, { endpoint: 'annotation-broad', name: 'annotation/phenotype-broad' },
        (b) => size(b.diseases) + size(b.genes) > 0);
    } else if (kind < 0.34) {
      get(`/annotation/${pick(PHENOTYPES)}`, { endpoint: 'annotation', name: 'annotation/phenotype' },
        (b) => size(b.diseases) + size(b.genes) > 0);
    } else if (kind < 0.67) {
      get(`/annotation/${pick(GENES)}`, { endpoint: 'annotation', name: 'annotation/gene' },
        (b) => size(b.diseases) + size(b.phenotypes) > 0);
    } else {
      get(`/annotation/${pick(DISEASES)}`, { endpoint: 'annotation', name: 'annotation/disease' },
        (b) => b.disease != null);
    }
  } else if (r < 0.73) {
    get(`/search/gene?q=${pick(GENE_TERMS)}`, { endpoint: 'search', name: 'search/gene' },
      (b) => size(b.results) > 0);
  } else if (r < 0.99) {
    get(`/search/disease?q=${pick(DISEASE_TERMS)}`, { endpoint: 'search', name: 'search/disease' },
      (b) => size(b.results) > 0);
  } else if (r < 0.995) {
    const terms = [pick(PHENOTYPES), pick(PHENOTYPES)].join(',');
    get(`/search/disease/intersect?p=${terms}`, { endpoint: 'intersect', name: 'search/disease/intersect' },
      null);
  } else {
    get(`/annotation/${pick(DISEASES)}/download/gene`, { endpoint: 'download', name: 'annotation/download' },
      null);
  }
}
