// This file is used to declare module types
declare module 'express' {
    export * from 'express';
}

declare module 'cors' {
    import { RequestHandler } from 'express';
    
    interface CorsOptions {
        origin?: boolean | string | string[] | RegExp | RegExp[] | ((origin: string, callback: (err: Error | null, allow?: boolean) => void) => void);
        methods?: string | string[];
        allowedHeaders?: string | string[];
        exposedHeaders?: string | string[];
        credentials?: boolean;
        maxAge?: number;
        preflightContinue?: boolean;
        optionsSuccessStatus?: number;
    }
    
    function cors(options?: CorsOptions): RequestHandler;
    export = cors;
}

declare module 'morgan' {
    import { RequestHandler } from 'express';
    
    function morgan(format: string, options?: any): RequestHandler;
    export = morgan;
}

declare module 'express-rate-limit' {
    import { RequestHandler } from 'express';
    
    interface RateLimitOptions {
        windowMs?: number;
        max?: number;
        message?: string | object;
        statusCode?: number;
        headers?: boolean;
        keyGenerator?: (req: any) => string;
        skip?: (req: any) => boolean;
        handler?: (req: any, res: any, next: any) => void;
        onLimitReached?: (req: any, res: any, options: RateLimitOptions) => void;
        standardHeaders?: boolean;
        legacyHeaders?: boolean;
    }
    
    function rateLimit(options?: RateLimitOptions): RequestHandler;
    export = rateLimit;
}

declare module 'rate-limiter-flexible' {
    export class RateLimiterMemory {
        constructor(options: {
            points: number;
            duration: number;
            blockDuration?: number;
        });
        consume(key: string, points?: number): Promise<any>;
        block(key: string, seconds?: number): Promise<any>;
    }
}

// Interface for exchange API responses
interface ExchangeBalance {
    [asset: string]: {
        free: string | number;
        locked: string | number;
        total: string | number;
    };
}

// Binance US specific types
interface BinanceUSBalance {
    asset: string;
    free: string;
    locked: string;
}

interface BinanceUSAccountInfo {
    makerCommission: number;
    takerCommission: number;
    buyerCommission: number;
    sellerCommission: number;
    canTrade: boolean;
    canWithdraw: boolean;
    canDeposit: boolean;
    updateTime: number;
    accountType: string;
    balances: BinanceUSBalance[];
    permissions: string[];
}

// Kraken specific types
interface KrakenBalance {
    [asset: string]: string;
}

// Coinbase specific types
interface CoinbaseAccount {
    id: string;
    name: string;
    primary: boolean;
    type: string;
    currency: {
        code: string;
        name: string;
    };
    balance: {
        amount: string;
        currency: string;
    };
    created_at: string;
    updated_at: string;
    resource: string;
    resource_path: string;
}

// Uniswap specific types
interface UniswapPosition {
    tokenId: string;
    token0: string;
    token1: string;
    fee: number;
    tickLower: number;
    tickUpper: number;
    liquidity: string;
}
