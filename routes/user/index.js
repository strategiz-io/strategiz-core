const express = require('express');
const router = express.Router();
const admin = require('firebase-admin');

// Get user profile
router.get('/:userId', async (req, res) => {
  try {
    const userId = req.params.userId;
    
    // Verify user exists in Firebase
    const userRecord = await admin.auth().getUser(userId);
    
    // Return user profile
    return res.json({
      id: userRecord.uid,
      email: userRecord.email,
      displayName: userRecord.displayName,
      photoURL: userRecord.photoURL,
      emailVerified: userRecord.emailVerified,
      createdAt: userRecord.metadata.creationTime
    });
  } catch (error) {
    console.error('Error fetching user profile:', error);
    return res.status(500).json({ error: 'Failed to fetch user profile' });
  }
});

// Update user exchange credentials
router.post('/:userId/credentials', async (req, res) => {
  try {
    const userId = req.params.userId;
    const { exchange, credentials } = req.body;
    
    if (!exchange || !credentials) {
      return res.status(400).json({ error: 'Exchange and credentials are required' });
    }
    
    // Store credentials in Firestore
    const db = admin.firestore();
    await db.collection('users').doc(userId).collection('credentials').doc(exchange).set({
      ...credentials,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    
    return res.json({ success: true });
  } catch (error) {
    console.error('Error updating user credentials:', error);
    return res.status(500).json({ error: 'Failed to update user credentials' });
  }
});

// Get user exchange credentials
router.get('/:userId/credentials/:exchange', async (req, res) => {
  try {
    const userId = req.params.userId;
    const exchange = req.params.exchange;
    
    // Get credentials from Firestore
    const db = admin.firestore();
    const doc = await db.collection('users').doc(userId).collection('credentials').doc(exchange).get();
    
    if (!doc.exists) {
      return res.status(404).json({ error: 'Credentials not found' });
    }
    
    return res.json(doc.data());
  } catch (error) {
    console.error('Error fetching user credentials:', error);
    return res.status(500).json({ error: 'Failed to fetch user credentials' });
  }
});

module.exports = router;
