require('dotenv').config();
const express = require('express');
const { Configuration, PlaidApi, PlaidEnvironments } = require('plaid');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.json());

const configuration = new Configuration({
    basePath: PlaidEnvironments[process.env.PLAID_ENV],
    baseOptions: {
        headers: {
            'PLAID-CLIENT-ID': process.env.PLAID_CLIENT_ID,
            'PLAID-SECRET': process.env.PLAID_SECRET,
        },
    },
});

const plaidClient = new PlaidApi(configuration);

// Create a link token
app.post('/api/create_link_token', async (req, res) => {
    try {
        const request = {
            user: { client_user_id: req.body.userId },
            client_name: 'SpendWise',
            products: process.env.PLAID_PRODUCTS.split(','),
            country_codes: process.env.PLAID_COUNTRY_CODES.split(','),
            language: 'en',
        };

        const response = await plaidClient.linkTokenCreate(request);
        res.json(response.data);
    } catch (error) {
        console.error('Error creating link token:', error);
        res.status(500).json({ error: error.message });
    }
});

// Exchange public token for access token
app.post('/api/exchange_public_token', async (req, res) => {
    try {
        const { public_token } = req.body;
        const response = await plaidClient.itemPublicTokenExchange({
            public_token: public_token,
        });
        res.json(response.data);
    } catch (error) {
        console.error('Error exchanging public token:', error);
        res.status(500).json({ error: error.message });
    }
});

// Get account balances
app.post('/api/accounts/balance', async (req, res) => {
    try {
        const { access_token } = req.body;
        const response = await plaidClient.accountsBalanceGet({
            access_token: access_token,
        });
        res.json(response.data);
    } catch (error) {
        console.error('Error getting account balances:', error);
        res.status(500).json({ error: error.message });
    }
});

// Get transactions
app.post('/api/transactions', async (req, res) => {
    try {
        const { access_token, start_date, end_date } = req.body;
        const response = await plaidClient.transactionsGet({
            access_token: access_token,
            start_date: start_date,
            end_date: end_date,
        });
        res.json(response.data);
    } catch (error) {
        console.error('Error getting transactions:', error);
        res.status(500).json({ error: error.message });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
}); 