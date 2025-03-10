import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import axios from 'axios';
import { Box, Typography, CircularProgress, Paper, Button } from '@mui/material';
import ReactJson from 'react-json-view';

/**
 * Admin page for viewing completely unmodified raw data from Coinbase API
 */
const AdminCoinbasePage: React.FC = () => {
  const { currentUser } = useAuth();
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [rawData, setRawData] = useState<any>(null);
  const [refreshCounter, setRefreshCounter] = useState<number>(0);

  useEffect(() => {
    const fetchRawData = async () => {
      if (!currentUser) return;
      
      setLoading(true);
      setError(null);
      
      try {
        // Fetch completely unmodified raw data from Coinbase API
        const response = await axios.get(
          `${process.env.REACT_APP_API_URL}/api/coinbase/raw-data/${currentUser.uid}`
        );
        
        // Set the completely unmodified raw data
        setRawData(response.data);
      } catch (err: any) {
        console.error('Error fetching Coinbase raw data:', err);
        setError(
          err.response?.data?.error || 
          err.message || 
          'Failed to fetch Coinbase raw data'
        );
      } finally {
        setLoading(false);
      }
    };
    
    fetchRawData();
  }, [currentUser, refreshCounter]);
  
  const handleRefresh = () => {
    setRefreshCounter(prev => prev + 1);
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
        Coinbase Raw Data (Admin)
      </Typography>
      <Typography variant="subtitle1" gutterBottom>
        This page displays the completely unmodified raw data from the Coinbase API.
      </Typography>
      
      <Button 
        variant="contained" 
        color="primary" 
        onClick={handleRefresh}
        sx={{ mb: 2 }}
        disabled={loading}
      >
        Refresh Data
      </Button>
      
      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}>
          <CircularProgress />
        </Box>
      ) : error ? (
        <Paper sx={{ p: 3, bgcolor: '#fff4f4', mb: 3 }}>
          <Typography color="error">{error}</Typography>
        </Paper>
      ) : (
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
      )}
      
      <Box sx={{ mt: 4 }}>
        <Typography variant="h6">Notes:</Typography>
        <Typography variant="body1">
          • This page shows the completely unmodified response from the Coinbase API.
        </Typography>
        <Typography variant="body1">
          • No transformations or mappings are applied to the data.
        </Typography>
        <Typography variant="body1">
          • This is useful for debugging and understanding the raw API response.
        </Typography>
      </Box>
    </Box>
  );
};

export default AdminCoinbasePage;
