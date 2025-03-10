import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import axios from 'axios';
import { Box, Typography, CircularProgress, Paper, Button, TextField } from '@mui/material';
import ReactJson from 'react-json-view';

/**
 * Admin page for viewing completely unmodified raw data from Uniswap
 */
const AdminUniswapPage: React.FC = () => {
  const { currentUser } = useAuth();
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [rawData, setRawData] = useState<any>(null);
  const [walletAddress, setWalletAddress] = useState<string>('');
  const [refreshCounter, setRefreshCounter] = useState<number>(0);

  useEffect(() => {
    // Load saved wallet address from localStorage if available
    const savedWalletAddress = localStorage.getItem('uniswapWalletAddress');
    if (savedWalletAddress) {
      setWalletAddress(savedWalletAddress);
    }
  }, []);

  const fetchRawData = async () => {
    if (!currentUser || !walletAddress) return;
    
    // Validate Ethereum address format
    if (!/^0x[a-fA-F0-9]{40}$/.test(walletAddress)) {
      setError('Invalid Ethereum wallet address format');
      return;
    }
    
    setLoading(true);
    setError(null);
    
    try {
      // Save wallet address to localStorage for convenience
      localStorage.setItem('uniswapWalletAddress', walletAddress);
      
      // Fetch completely unmodified raw data from Uniswap
      const response = await axios.get(
        `${process.env.REACT_APP_API_URL}/api/uniswap/raw-data/${currentUser.uid}/${walletAddress}`
      );
      
      // Set the completely unmodified raw data
      setRawData(response.data);
    } catch (err: any) {
      console.error('Error fetching Uniswap raw data:', err);
      setError(
        err.response?.data?.error || 
        err.message || 
        'Failed to fetch Uniswap raw data'
      );
    } finally {
      setLoading(false);
    }
  };
  
  useEffect(() => {
    if (walletAddress) {
      fetchRawData();
    }
  }, [currentUser, refreshCounter]);
  
  const handleRefresh = () => {
    setRefreshCounter(prev => prev + 1);
  };
  
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    fetchRawData();
  };
  
  if (!currentUser) {
    return (
      <Box sx={{ p: 3 }}>
        <Typography variant="h4">Please log in to view this page</Typography>
      </Box>
    );
  }
  
  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Uniswap Raw Data (Admin)
      </Typography>
      <Typography variant="subtitle1" gutterBottom>
        This page displays the completely unmodified raw data from Uniswap.
      </Typography>
      
      <Paper sx={{ p: 3, mb: 3 }}>
        <form onSubmit={handleSubmit}>
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
            <TextField
              label="Ethereum Wallet Address"
              variant="outlined"
              fullWidth
              value={walletAddress}
              onChange={(e) => setWalletAddress(e.target.value)}
              placeholder="0x..."
              sx={{ mr: 2 }}
            />
            <Button 
              type="submit" 
              variant="contained" 
              color="primary"
              disabled={loading || !walletAddress}
            >
              Fetch Data
            </Button>
          </Box>
        </form>
        
        <Button 
          variant="outlined" 
          color="primary" 
          onClick={handleRefresh}
          disabled={loading || !walletAddress}
          sx={{ mt: 1 }}
        >
          Refresh Data
        </Button>
      </Paper>
      
      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}>
          <CircularProgress />
        </Box>
      ) : error ? (
        <Paper sx={{ p: 3, bgcolor: '#fff4f4', mb: 3 }}>
          <Typography color="error">{error}</Typography>
        </Paper>
      ) : rawData ? (
        <Paper sx={{ p: 3, overflow: 'auto' }}>
          <ReactJson 
            src={rawData} 
            theme="rjv-default" 
            displayDataTypes={false} 
            enableClipboard={true}
            collapsed={1}
            name={false}
          />
        </Paper>
      ) : (
        <Paper sx={{ p: 3, textAlign: 'center' }}>
          <Typography>Enter an Ethereum wallet address to view Uniswap data</Typography>
        </Paper>
      )}
      
      <Box sx={{ mt: 4 }}>
        <Typography variant="h6">Notes:</Typography>
        <Typography variant="body1">
          • This page shows the completely unmodified response from the Uniswap integration.
        </Typography>
        <Typography variant="body1">
          • Raw position data is fetched directly from the Ethereum blockchain.
        </Typography>
        <Typography variant="body1">
          • No transformations or mappings are applied to the data.
        </Typography>
        <Typography variant="body1">
          • This is useful for debugging and understanding the raw blockchain data.
        </Typography>
      </Box>
    </Box>
  );
};

export default AdminUniswapPage;
