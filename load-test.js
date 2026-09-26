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

function randomPhone() {
    return '9' + Math.floor(100000000 + Math.random() * 900000000);
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

            rate: 50,

            timeUnit: '1s',

            duration: '30s',

            preAllocatedVUs: 500,

            maxVUs: 5000,
        },
    },

    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1000'],
    },
};

export default function () {

    const randomId = randomString(8);

    const product =
        products[Math.floor(Math.random() * products.length)];

    const orderId =
        `ORD-${Date.now()}-${__VU}-${__ITER}-${randomId}`;

    const amount =
        Math.floor(Math.random() * 100000) + 1000;

    const customerName =
        `Customer-${randomString(6)}`;

    const phone =
        randomPhone();

    const email =
        `customer-${randomString(8).toLowerCase()}@example.com`;

    const whatsapp =
        phone;

    const payload = JSON.stringify({

        orderId: orderId,

        product: product,

        amount: amount,

        customerName: customerName,

        email: email,

        phone: phone,

        whatsapp: whatsapp
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