import http from 'k6/http';
import { check } from 'k6';

function randomString(length) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let result = '';

    for (let i = 0; i < length; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
    }

    return result;
}

const products = [
    'Sony ZV-E10',
    'Sony A6400',
    'Canon R50',
    'Fujifilm X-M5',
    'MacBook Air M3',
    'iPhone 17',
    'Samsung S25 Ultra',
    'DJI Osmo Pocket',
    'GoPro Hero',
    'Apple Watch'
];

export const options = {
    scenarios: {
        orders: {
            executor: 'constant-arrival-rate',

            // 10,000 requests every second
            rate: 50,

            timeUnit: '1s',

            // Run for 30 seconds
            duration: '30s',

            // Start with 500 virtual users
            preAllocatedVUs: 500,

            // Maximum users k6 can create
            maxVUs: 5000,
        },
    },

    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1000'],
    },
};

export default function () {

    const product =
        products[Math.floor(Math.random() * products.length)];

    const orderId =
        `ORD-${Date.now()}-${__VU}-${__ITER}-${randomString(6)}`;

    const amount =
        Math.floor(Math.random() * 100000) + 1000;

    const payload = JSON.stringify({
        orderId: orderId,
        product: product,
        amount: amount
    });

    const params = {
        headers: {
            'Content-Type': 'application/json'
        }
    };

    const response = http.post(
        'http://localhost:8080/api/orders',
        payload,
        params
    );

    check(response, {
        'HTTP 200': (r) => r.status === 200
    });
}