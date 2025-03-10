# Coinbase Integration Troubleshooting Guide

This document provides solutions for common issues encountered when working with the Coinbase API integration.

## Authentication Issues

### Issue: API Key Invalid

**Symptoms:**
- 401 Unauthorized responses
- Error message: "Invalid API Key"

**Solutions:**
1. Verify the API key is correctly copied from the Coinbase developer dashboard
2. Ensure the API key has the correct permissions (read access to accounts)
3. Check if the API key has been revoked or expired
4. Regenerate a new API key if necessary

### Issue: Invalid Signature

**Symptoms:**
- 401 Unauthorized responses
- Error message: "Invalid signature"

**Solutions:**
1. Verify the API secret is correctly copied
2. Ensure the timestamp in the request is within 30 seconds of the Coinbase server time
3. Check that the signature is being generated with the correct algorithm (HMAC SHA256)
4. Verify the request body is being properly included in the signature

## Rate Limiting Issues

### Issue: Too Many Requests

**Symptoms:**
- 429 Too Many Requests responses
- Error message: "Rate limit exceeded"

**Solutions:**
1. Implement exponential backoff for retries
2. Add caching to reduce the number of API calls
3. Optimize code to batch requests where possible
4. Ensure rate limiting is properly implemented in the application

## Data Issues

### Issue: Missing Account Data

**Symptoms:**
- Incomplete or missing account information
- Some assets not appearing in the balance

**Solutions:**
1. Verify API permissions include access to all required accounts
2. Check if accounts are hidden in the Coinbase UI (hidden accounts may not appear in API responses)
3. Ensure pagination is properly handled for accounts with many assets
4. Verify the account is not restricted or under review

### Issue: Incorrect USD Values

**Symptoms:**
- USD values for assets appear incorrect or outdated

**Solutions:**
1. Check if the price data source is up-to-date
2. Verify the calculation logic for converting crypto to USD
3. Ensure the correct trading pairs are being used for price conversion
4. Implement a fallback price source for assets with low liquidity

## Connection Issues

### Issue: API Timeout

**Symptoms:**
- Requests take too long to complete
- Connection timeout errors

**Solutions:**
1. Increase request timeout settings
2. Implement circuit breaker pattern to prevent cascading failures
3. Add retry logic with exponential backoff
4. Check Coinbase API status page for outages

### Issue: Network Connectivity

**Symptoms:**
- Intermittent connection failures
- ECONNREFUSED or similar errors

**Solutions:**
1. Verify network connectivity to api.coinbase.com
2. Check firewall settings to ensure outbound connections are allowed
3. Try an alternative network connection
4. Implement robust error handling for network issues

## Environment Configuration Issues

### Issue: Missing Environment Variables

**Symptoms:**
- Application fails to start or authenticate
- Error messages about missing configuration

**Solutions:**
1. Verify all required environment variables are set:
   - COINBASE_API_KEY
   - COINBASE_API_SECRET
   - COINBASE_API_PASSPHRASE
2. Check that environment variables are loaded correctly
3. Ensure environment files (.env) are in the correct location
4. Add validation for required configuration at startup

## Common Error Codes

| Error Code | Description | Solution |
|------------|-------------|----------|
| 400 | Bad Request | Check request parameters and format |
| 401 | Unauthorized | Verify API credentials |
| 403 | Forbidden | Check API permissions |
| 404 | Not Found | Verify endpoint URL and resource existence |
| 429 | Too Many Requests | Implement rate limiting and backoff |
| 500 | Internal Server Error | Contact Coinbase support if persistent |
| 503 | Service Unavailable | Check Coinbase status page and retry later |

## Getting Additional Help

If you continue to experience issues after trying these troubleshooting steps:

1. Check the [Coinbase API Documentation](https://docs.cloud.coinbase.com/)
2. Visit the [Coinbase Developer Forum](https://forums.coinbase.com/)
3. Contact Coinbase Support with detailed error information
4. Review the application logs for more specific error details
