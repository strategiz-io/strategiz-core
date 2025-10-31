#!/usr/bin/env python3
"""
Generate SVG diagrams for Strategiz Core documentation.
These can be directly displayed in browsers.
"""

def create_architecture_overview_svg():
    """Create high-level architecture SVG diagram."""
    return '''<?xml version="1.0" encoding="UTF-8"?>
<svg width="1200" height="800" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <style>
      .title { font: bold 24px sans-serif; fill: #FFFFFF; }
      .box { stroke: #FFFFFF; stroke-width: 2; }
      .box-blue { fill: #00BFFF; }
      .box-green { fill: #39FF14; }
      .box-purple { fill: #BF00FF; }
      .text-white { font: 14px sans-serif; fill: #FFFFFF; text-anchor: middle; }
      .text-dark { font: 14px sans-serif; fill: #1a1a1a; text-anchor: middle; }
      .arrow { stroke: #FFFFFF; stroke-width: 2; fill: none; marker-end: url(#arrowhead); }
    </style>
    <marker id="arrowhead" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto">
      <polygon points="0 0, 10 3, 0 6" fill="#FFFFFF" />
    </marker>
  </defs>

  <rect width="1200" height="800" fill="#1a1a1a"/>

  <!-- Title -->
  <text x="600" y="50" class="title" text-anchor="middle">Strategiz Core - High-Level Architecture</text>

  <!-- Client Layer -->
  <rect x="440" y="120" width="300" height="60" class="box box-blue" rx="5"/>
  <text x="590" y="155" class="text-white">Client Applications</text>

  <!-- API Gateway -->
  <rect x="440" y="220" width="300" height="60" class="box box-purple" rx="5"/>
  <text x="590" y="245" class="text-white">API Gateway /</text>
  <text x="590" y="265" class="text-white">Load Balancer</text>

  <!-- Service Layer -->
  <rect x="440" y="320" width="300" height="60" class="box box-green" rx="5"/>
  <text x="590" y="345" class="text-dark">Service Layer</text>
  <text x="590" y="365" class="text-dark">(REST Controllers)</text>

  <!-- Business Layer -->
  <rect x="440" y="420" width="300" height="60" class="box box-green" rx="5"/>
  <text x="590" y="445" class="text-dark">Business Logic Layer</text>
  <text x="590" y="465" class="text-dark">(Services)</text>

  <!-- Client Integration Layer -->
  <rect x="440" y="520" width="300" height="60" class="box box-green" rx="5"/>
  <text x="590" y="545" class="text-dark">Client Integration Layer</text>
  <text x="590" y="565" class="text-dark">(Provider Clients)</text>

  <!-- Data Layer - Firestore -->
  <ellipse cx="270" cy="670" rx="70" ry="50" class="box box-blue"/>
  <text x="270" y="665" class="text-white">Firestore</text>
  <text x="270" y="685" class="text-white">(User Data)</text>

  <!-- Data Layer - Vault -->
  <ellipse cx="590" cy="670" rx="70" ry="50" class="box box-purple"/>
  <text x="590" y="665" class="text-white">Vault</text>
  <text x="590" y="685" class="text-white">(Secrets)</text>

  <!-- External Services -->
  <ellipse cx="920" cy="560" rx="100" ry="60" class="box box-blue"/>
  <text x="920" y="550" class="text-white">External Providers</text>
  <text x="920" y="570" class="text-white">(Coinbase, Binance,</text>
  <text x="920" y="590" class="text-white">etc.)</text>

  <!-- Arrows -->
  <line x1="590" y1="180" x2="590" y2="220" class="arrow"/>
  <line x1="590" y1="280" x2="590" y2="320" class="arrow"/>
  <line x1="590" y1="380" x2="590" y2="420" class="arrow"/>
  <line x1="590" y1="480" x2="590" y2="520" class="arrow"/>

  <!-- Business to Firestore -->
  <path d="M 440 450 Q 350 450 300 620" class="arrow" stroke="#00BFFF"/>
  <text x="360" y="530" font-size="12" fill="#00BFFF">Read/Write</text>

  <!-- Client to Vault -->
  <line x1="590" y1="580" x2="590" y2="620" class="arrow" stroke="#BF00FF"/>
  <text x="620" y="605" font-size="12" fill="#BF00FF">Secrets</text>

  <!-- Client to External -->
  <path d="M 740 550 Q 820 550 820 560" class="arrow" stroke="#39FF14"/>
  <text x="780" y="545" font-size="12" fill="#39FF14">OAuth/API</text>
</svg>'''

def create_layered_architecture_svg():
    """Create layered architecture SVG diagram."""
    return '''<?xml version="1.0" encoding="UTF-8"?>
<svg width="1200" height="900" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <style>
      .title { font: bold 24px sans-serif; fill: #FFFFFF; }
      .layer-title { font: bold 16px sans-serif; fill: #FFFFFF; }
      .layer-title-dark { font: bold 16px sans-serif; fill: #1a1a1a; }
      .text { font: 14px sans-serif; fill: #FFFFFF; }
      .text-dark { font: 14px sans-serif; fill: #1a1a1a; }
      .box { stroke: #FFFFFF; stroke-width: 3; }
      .box-green { stroke: #1a1a1a; stroke-width: 3; }
      .arrow { stroke: #FFFFFF; stroke-width: 3; fill: none; marker-end: url(#arrowhead); }
    </style>
    <marker id="arrowhead" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto">
      <polygon points="0 0, 10 3, 0 6" fill="#FFFFFF" />
    </marker>
  </defs>

  <rect width="1200" height="900" fill="#1a1a1a"/>

  <!-- Title -->
  <text x="600" y="50" class="title" text-anchor="middle">Strategiz Core - Layered Architecture</text>

  <!-- Layer 1: Service -->
  <rect x="120" y="120" width="940" height="100" fill="#00BFFF" class="box" rx="5"/>
  <text x="140" y="145" class="layer-title">Layer 1: Service Layer (service/*)</text>
  <text x="140" y="165" class="text">REST Controllers, API Endpoints, Request/Response Models</text>
  <text x="140" y="190" class="text" font-size="12">• AuthenticationController  • ProviderConnectionController  • PortfolioController</text>

  <!-- Layer 2: Business -->
  <rect x="120" y="260" width="940" height="100" fill="#39FF14" class="box-green" rx="5"/>
  <text x="140" y="285" class="layer-title-dark">Layer 2: Business Logic (business/*)</text>
  <text x="140" y="305" class="text-dark">Core Business Logic, Orchestration, Provider-Specific Services</text>
  <text x="140" y="330" class="text-dark" font-size="12">• CoinbaseConnectionService  • BinanceConnectionService  • PortfolioAggregationService</text>

  <!-- Layer 3: Client -->
  <rect x="120" y="400" width="940" height="100" fill="#BF00FF" class="box" rx="5"/>
  <text x="140" y="425" class="layer-title">Layer 3: Client Integration (client/*)</text>
  <text x="140" y="445" class="text">External API Clients, OAuth Handlers, Data Transformation</text>
  <text x="140" y="470" class="text" font-size="12">• CoinbaseClient  • BinanceClient  • OAuth2TokenClient</text>

  <!-- Layer 4: Data -->
  <rect x="120" y="540" width="940" height="100" fill="#00BFFF" class="box" rx="5"/>
  <text x="140" y="565" class="layer-title">Layer 4: Data Access (data/*)</text>
  <text x="140" y="585" class="text">Repositories, DAOs, Data Models, Persistence</text>
  <text x="140" y="610" class="text" font-size="12">• FirestoreService  • VaultService  • UserRepository</text>

  <!-- External Databases -->
  <ellipse cx="240" cy="720" rx="60" ry="40" fill="#1a1a1a" stroke="#39FF14" stroke-width="2"/>
  <text x="240" y="720" font-size="14" fill="#39FF14" text-anchor="middle">Firestore</text>

  <ellipse cx="590" cy="720" rx="60" ry="40" fill="#1a1a1a" stroke="#BF00FF" stroke-width="2"/>
  <text x="590" y="720" font-size="14" fill="#BF00FF" text-anchor="middle">Vault</text>

  <!-- Arrows between layers -->
  <line x1="590" y1="220" x2="590" y2="260" class="arrow"/>
  <line x1="590" y1="360" x2="590" y2="400" class="arrow"/>
  <line x1="590" y1="500" x2="590" y2="540" class="arrow"/>

  <!-- Data layer connections -->
  <line x1="240" y1="640" x2="240" y2="680" class="arrow" stroke="#39FF14"/>
  <line x1="590" y1="640" x2="590" y2="680" class="arrow" stroke="#BF00FF"/>
</svg>'''

def create_provider_connection_flow_svg():
    """Create provider connection flow SVG diagram."""
    return '''<?xml version="1.0" encoding="UTF-8"?>
<svg width="1400" height="700" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <style>
      .title { font: bold 24px sans-serif; fill: #FFFFFF; }
      .step { stroke: #FFFFFF; stroke-width: 2; }
      .step-blue { fill: #00BFFF; }
      .step-green { fill: #39FF14; }
      .step-purple { fill: #BF00FF; }
      .text-white { font: 12px sans-serif; fill: #FFFFFF; text-anchor: middle; }
      .text-dark { font: 12px sans-serif; fill: #1a1a1a; text-anchor: middle; }
      .arrow { stroke: #FFFFFF; stroke-width: 2; fill: none; marker-end: url(#arrowhead); }
    </style>
    <marker id="arrowhead" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto">
      <polygon points="0 0, 10 3, 0 6" fill="#FFFFFF" />
    </marker>
  </defs>

  <rect width="1400" height="700" fill="#1a1a1a"/>

  <!-- Title -->
  <text x="700" y="40" class="title" text-anchor="middle">Provider Connection Flow (OAuth 2.0)</text>

  <!-- Flow Steps -->
  <!-- Step 1 -->
  <rect x="80" y="100" width="140" height="60" class="step step-blue" rx="5"/>
  <text x="150" y="125" class="text-white">1. Click "Connect</text>
  <text x="150" y="145" class="text-white">Coinbase"</text>

  <!-- Step 2 -->
  <rect x="280" y="100" width="140" height="60" class="step step-green" rx="5"/>
  <text x="350" y="125" class="text-dark">2. Initiate</text>
  <text x="350" y="145" class="text-dark">OAuth</text>

  <!-- Step 3 -->
  <rect x="480" y="100" width="140" height="60" class="step step-purple" rx="5"/>
  <text x="550" y="120" class="text-white">3. Request</text>
  <text x="550" y="135" class="text-white">Authorization</text>
  <text x="550" y="150" class="text-white">URL</text>

  <!-- Step 4 -->
  <rect x="680" y="100" width="140" height="60" class="step step-blue" rx="5"/>
  <text x="750" y="125" class="text-white">4. Generate URL</text>
  <text x="750" y="145" class="text-white">+ State</text>

  <!-- Step 5 -->
  <rect x="880" y="100" width="140" height="60" class="step step-blue" rx="5"/>
  <text x="950" y="125" class="text-white">5. Redirect to</text>
  <text x="950" y="145" class="text-white">Provider</text>

  <!-- Step 6 -->
  <rect x="880" y="220" width="140" height="60" class="step step-blue" rx="5"/>
  <text x="950" y="245" class="text-white">6. User</text>
  <text x="950" y="265" class="text-white">Approves</text>

  <!-- Step 7 -->
  <rect x="680" y="340" width="140" height="60" class="step step-green" rx="5"/>
  <text x="750" y="360" class="text-dark">7. Callback with</text>
  <text x="750" y="380" class="text-dark">Auth Code</text>

  <!-- Step 8 -->
  <rect x="480" y="460" width="140" height="60" class="step step-purple" rx="5"/>
  <text x="550" y="480" class="text-white">8. Exchange Code</text>
  <text x="550" y="500" class="text-white">for Token</text>

  <!-- Step 9 -->
  <rect x="280" y="580" width="140" height="60" class="step step-purple" rx="5"/>
  <text x="350" y="605" class="text-white">9. Store in Vault</text>

  <!-- Step 10 -->
  <rect x="80" y="580" width="140" height="60" class="step step-purple" rx="5"/>
  <text x="150" y="600" class="text-white">10. Fetch</text>
  <text x="150" y="620" class="text-white">Portfolio</text>

  <!-- Arrows -->
  <line x1="220" y1="130" x2="280" y2="130" class="arrow"/>
  <line x1="420" y1="130" x2="480" y2="130" class="arrow"/>
  <line x1="620" y1="130" x2="680" y2="130" class="arrow"/>
  <line x1="820" y1="130" x2="880" y2="130" class="arrow"/>
  <line x1="950" y1="160" x2="950" y2="220" class="arrow"/>
  <line x1="880" y1="250" x2="820" y2="370" class="arrow"/>
  <line x1="680" y1="370" x2="620" y2="490" class="arrow"/>
  <line x1="480" y1="490" x2="420" y2="610" class="arrow"/>
  <line x1="280" y1="610" x2="220" y2="610" class="arrow"/>
</svg>'''

def create_authentication_flow_svg():
    """Create authentication flow SVG diagram."""
    return '''<?xml version="1.0" encoding="UTF-8"?>
<svg width="1200" height="760" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <style>
      .title { font: bold 24px sans-serif; fill: #FFFFFF; }
      .box { stroke: #FFFFFF; stroke-width: 2; }
      .box-blue { fill: #00BFFF; }
      .box-green { fill: #39FF14; }
      .box-purple { fill: #BF00FF; }
      .box-red { fill: #FF0000; }
      .text-white { font: 14px sans-serif; fill: #FFFFFF; text-anchor: middle; }
      .text-dark { font: 14px sans-serif; fill: #1a1a1a; text-anchor: middle; }
      .arrow { stroke: #FFFFFF; stroke-width: 2; fill: none; marker-end: url(#arrowhead); }
      .arrow-green { stroke: #39FF14; stroke-width: 2; fill: none; marker-end: url(#arrowhead-green); }
      .arrow-red { stroke: #FF0000; stroke-width: 2; fill: none; marker-end: url(#arrowhead-red); }
    </style>
    <marker id="arrowhead" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto">
      <polygon points="0 0, 10 3, 0 6" fill="#FFFFFF" />
    </marker>
    <marker id="arrowhead-green" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto">
      <polygon points="0 0, 10 3, 0 6" fill="#39FF14" />
    </marker>
    <marker id="arrowhead-red" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto">
      <polygon points="0 0, 10 3, 0 6" fill="#FF0000" />
    </marker>
  </defs>

  <rect width="1200" height="760" fill="#1a1a1a"/>

  <!-- Title -->
  <text x="600" y="50" class="title" text-anchor="middle">Multi-Factor Authentication Flow</text>

  <!-- User Login -->
  <rect x="520" y="120" width="140" height="60" class="box box-blue" rx="5"/>
  <text x="590" y="155" class="text-white">User Login</text>

  <!-- Authentication Methods -->
  <rect x="200" y="240" width="140" height="60" class="box box-green" rx="5"/>
  <text x="270" y="260" class="text-dark">Passkey</text>
  <text x="270" y="280" class="text-dark">(WebAuthn)</text>

  <rect x="400" y="240" width="140" height="60" class="box box-green" rx="5"/>
  <text x="470" y="260" class="text-dark">TOTP</text>
  <text x="470" y="280" class="text-dark">(Google Auth)</text>

  <rect x="600" y="240" width="140" height="60" class="box box-green" rx="5"/>
  <text x="670" y="270" class="text-dark">SMS OTP</text>

  <rect x="800" y="240" width="140" height="60" class="box box-green" rx="5"/>
  <text x="870" y="260" class="text-dark">Social OAuth</text>
  <text x="870" y="280" class="text-dark">(Google)</text>

  <!-- Verify Credentials (Diamond) -->
  <polygon points="590,360 680,400 590,440 500,400" fill="#BF00FF" stroke="#FFFFFF" stroke-width="2"/>
  <text x="590" y="395" class="text-white">Verify</text>
  <text x="590" y="415" class="text-white">Credentials</text>

  <!-- Generate JWT -->
  <rect x="520" y="500" width="140" height="60" class="box box-green" rx="5"/>
  <text x="590" y="525" class="text-dark">Generate JWT</text>
  <text x="590" y="545" class="text-dark">+ Session</text>

  <!-- Auth Failed -->
  <rect x="780" y="370" width="140" height="60" class="box box-red" rx="5"/>
  <text x="850" y="395" class="text-white">Auth Failed</text>
  <text x="850" y="415" class="text-white">Rate Limit</text>

  <!-- Dashboard -->
  <rect x="520" y="620" width="140" height="60" class="box box-blue" rx="5"/>
  <text x="590" y="645" class="text-white">Redirect to</text>
  <text x="590" y="665" class="text-white">Dashboard</text>

  <!-- Arrows from login to methods -->
  <line x1="520" y1="150" x2="340" y2="240" class="arrow"/>
  <line x1="560" y1="180" x2="470" y2="240" class="arrow"/>
  <line x1="590" y1="180" x2="670" y2="240" class="arrow"/>
  <line x1="620" y1="150" x2="840" y2="240" class="arrow"/>

  <!-- Arrows from methods to verify -->
  <line x1="300" y1="300" x2="540" y2="380" class="arrow"/>
  <line x1="470" y1="300" x2="560" y2="380" class="arrow"/>
  <line x1="670" y1="300" x2="600" y2="380" class="arrow"/>
  <line x1="870" y1="300" x2="640" y2="380" class="arrow"/>

  <!-- Success path -->
  <line x1="590" y1="440" x2="590" y2="500" class="arrow-green"/>
  <text x="630" y="475" font-size="12" fill="#39FF14">Valid</text>

  <!-- Failure path -->
  <line x1="680" y1="400" x2="780" y2="400" class="arrow-red"/>
  <text x="720" y="390" font-size="12" fill="#FF0000">Invalid</text>

  <!-- JWT to Dashboard -->
  <line x1="590" y1="560" x2="590" y2="620" class="arrow"/>
</svg>'''

# Generate SVG diagrams
if __name__ == '__main__':
    print("Generating SVG diagrams for Strategiz Core...")

    diagrams = [
        ('Architecture Overview', create_architecture_overview_svg, 'architecture-overview.svg'),
        ('Layered Architecture', create_layered_architecture_svg, 'layered-architecture.svg'),
        ('Provider Connection Flow', create_provider_connection_flow_svg, 'provider-connection-flow.svg'),
        ('Authentication Flow', create_authentication_flow_svg, 'authentication-flow.svg'),
    ]

    base_path = '/Users/cuztomizer/Documents/GitHub/strategiz-core/docs/diagrams/'

    for name, func, filename in diagrams:
        print(f"  Creating {name}...")
        svg_content = func()
        with open(base_path + filename, 'w') as f:
            f.write(svg_content)
        print(f"  Created: {base_path}{filename}")

    print("\n✅ All SVG diagrams generated successfully!")
    print("These SVG files can be viewed directly in browsers.")
