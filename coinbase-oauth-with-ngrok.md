# Complete Coinbase OAuth Setup with ngrok

## 1. Configure ngrok (one-time)
```bash
./configure-ngrok.sh YOUR_AUTH_TOKEN
```

## 2. Start ngrok Tunnel (Terminal 1)
```bash
ngrok http https://localhost:8443
```
Copy the HTTPS URL shown (e.g., https://abc123.ngrok-free.app)

## 3. Update Coinbase OAuth App
1. Go to: https://www.coinbase.com/settings/api
2. Edit your OAuth application
3. Update redirect URI:
   - Remove: https://localhost:8443/v1/providers/callback/coinbase
   - Add: https://YOUR-NGROK-URL.ngrok-free.app/v1/providers/callback/coinbase
4. Save changes

## 4. Start Backend with ngrok (Terminal 2)
```bash
# Kill existing backend
pkill -f "java.*application-1.0-SNAPSHOT.jar"

# Start with ngrok URL
./start-backend-with-ngrok.sh https://YOUR-NGROK-URL.ngrok-free.app
```

## 5. Test OAuth Flow
1. Frontend remains at: http://localhost:3000
2. Click "Connect Coinbase" 
3. OAuth will redirect through ngrok URL
4. Success!

## Important Notes:
- ngrok URL changes each time you restart (unless you pay for custom domain)
- Update Coinbase redirect URI each time you get a new ngrok URL
- Keep ngrok terminal open while testing
