#!/usr/bin/env python3
"""
Generate Draw.io diagrams as editable PNG files for Strategiz Core documentation.
"""

import base64
import zlib
import xml.etree.ElementTree as ET
from io import BytesIO
from PIL import Image, PngImagePlugin
import urllib.parse

def create_drawio_xml(diagram_content):
    """Create a complete Draw.io XML document."""
    root = ET.Element('mxfile', {
        'host': 'app.diagrams.net',
        'modified': '2025-10-26T00:00:00.000Z',
        'agent': 'Strategiz Diagram Generator',
        'version': '22.0.0',
        'type': 'device'
    })

    diagram = ET.SubElement(root, 'diagram', {
        'id': 'strategiz-diagram',
        'name': 'Page-1'
    })

    # Add the diagram content (mxGraphModel)
    diagram.append(diagram_content)

    return ET.tostring(root, encoding='unicode')

def create_architecture_overview():
    """Create high-level architecture diagram."""
    graph_model = ET.Element('mxGraphModel', {
        'dx': '1422',
        'dy': '794',
        'grid': '1',
        'gridSize': '10',
        'guides': '1',
        'tooltips': '1',
        'connect': '1',
        'arrows': '1',
        'fold': '1',
        'page': '1',
        'pageScale': '1',
        'pageWidth': '1169',
        'pageHeight': '827',
        'math': '0',
        'shadow': '0'
    })

    root = ET.SubElement(graph_model, 'root')
    ET.SubElement(root, 'mxCell', {'id': '0'})
    ET.SubElement(root, 'mxCell', {'id': '1', 'parent': '0'})

    # Title
    cell = ET.SubElement(root, 'mxCell', {
        'id': '2',
        'value': 'Strategiz Core - High-Level Architecture',
        'style': 'text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;whiteSpace=wrap;rounded=0;fontSize=24;fontStyle=1;fontColor=#FFFFFF;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '340', 'y': '40', 'width': '500', 'height': '40', 'as': 'geometry'})

    # Client Layer
    cell = ET.SubElement(root, 'mxCell', {
        'id': '3',
        'value': 'Client Applications',
        'style': 'rounded=1;whiteSpace=wrap;html=1;fillColor=#00BFFF;strokeColor=#FFFFFF;strokeWidth=2;fontSize=16;fontColor=#FFFFFF;fontStyle=1;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '440', 'y': '120', 'width': '300', 'height': '60', 'as': 'geometry'})

    # API Gateway
    cell = ET.SubElement(root, 'mxCell', {
        'id': '4',
        'value': 'API Gateway / Load Balancer',
        'style': 'rounded=1;whiteSpace=wrap;html=1;fillColor=#BF00FF;strokeColor=#FFFFFF;strokeWidth=2;fontSize=16;fontColor=#FFFFFF;fontStyle=1;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '440', 'y': '220', 'width': '300', 'height': '60', 'as': 'geometry'})

    # Service Layer
    cell = ET.SubElement(root, 'mxCell', {
        'id': '5',
        'value': 'Service Layer\n(REST Controllers)',
        'style': 'rounded=1;whiteSpace=wrap;html=1;fillColor=#39FF14;strokeColor=#1a1a1a;strokeWidth=2;fontSize=14;fontColor=#1a1a1a;fontStyle=1;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '440', 'y': '320', 'width': '300', 'height': '60', 'as': 'geometry'})

    # Business Layer
    cell = ET.SubElement(root, 'mxCell', {
        'id': '6',
        'value': 'Business Logic Layer\n(Services)',
        'style': 'rounded=1;whiteSpace=wrap;html=1;fillColor=#39FF14;strokeColor=#1a1a1a;strokeWidth=2;fontSize=14;fontColor=#1a1a1a;fontStyle=1;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '440', 'y': '420', 'width': '300', 'height': '60', 'as': 'geometry'})

    # Client Integration Layer
    cell = ET.SubElement(root, 'mxCell', {
        'id': '7',
        'value': 'Client Integration Layer\n(Provider Clients)',
        'style': 'rounded=1;whiteSpace=wrap;html=1;fillColor=#39FF14;strokeColor=#1a1a1a;strokeWidth=2;fontSize=14;fontColor=#1a1a1a;fontStyle=1;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '440', 'y': '520', 'width': '300', 'height': '60', 'as': 'geometry'})

    # Data Layer - left side
    cell = ET.SubElement(root, 'mxCell', {
        'id': '8',
        'value': 'Firestore\n(User Data)',
        'style': 'shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;backgroundOutline=1;size=15;fillColor=#00BFFF;strokeColor=#FFFFFF;strokeWidth=2;fontSize=14;fontColor=#FFFFFF;fontStyle=1;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '200', 'y': '620', 'width': '140', 'height': '100', 'as': 'geometry'})

    # Data Layer - center
    cell = ET.SubElement(root, 'mxCell', {
        'id': '9',
        'value': 'Vault\n(Secrets)',
        'style': 'shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;backgroundOutline=1;size=15;fillColor=#BF00FF;strokeColor=#FFFFFF;strokeWidth=2;fontSize=14;fontColor=#FFFFFF;fontStyle=1;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '520', 'y': '620', 'width': '140', 'height': '100', 'as': 'geometry'})

    # External Services - right side
    cell = ET.SubElement(root, 'mxCell', {
        'id': '10',
        'value': 'External Providers\n(Coinbase, Binance, etc.)',
        'style': 'ellipse;shape=cloud;whiteSpace=wrap;html=1;fillColor=#00BFFF;strokeColor=#FFFFFF;strokeWidth=2;fontSize=14;fontColor=#FFFFFF;fontStyle=1;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '820', 'y': '500', 'width': '200', 'height': '120', 'as': 'geometry'})

    # Arrows - Client to API Gateway
    cell = ET.SubElement(root, 'mxCell', {
        'id': '11',
        'value': '',
        'style': 'edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#FFFFFF;strokeWidth=2;',
        'edge': '1',
        'parent': '1',
        'source': '3',
        'target': '4'
    })
    ET.SubElement(cell, 'mxGeometry', {'relative': '1', 'as': 'geometry'})

    # API Gateway to Service
    cell = ET.SubElement(root, 'mxCell', {
        'id': '12',
        'value': '',
        'style': 'edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#FFFFFF;strokeWidth=2;',
        'edge': '1',
        'parent': '1',
        'source': '4',
        'target': '5'
    })
    ET.SubElement(cell, 'mxGeometry', {'relative': '1', 'as': 'geometry'})

    # Service to Business
    cell = ET.SubElement(root, 'mxCell', {
        'id': '13',
        'value': '',
        'style': 'edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#FFFFFF;strokeWidth=2;',
        'edge': '1',
        'parent': '1',
        'source': '5',
        'target': '6'
    })
    ET.SubElement(cell, 'mxGeometry', {'relative': '1', 'as': 'geometry'})

    # Business to Client Layer
    cell = ET.SubElement(root, 'mxCell', {
        'id': '14',
        'value': '',
        'style': 'edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#FFFFFF;strokeWidth=2;',
        'edge': '1',
        'parent': '1',
        'source': '6',
        'target': '7'
    })
    ET.SubElement(cell, 'mxGeometry', {'relative': '1', 'as': 'geometry'})

    # Client Layer to External
    cell = ET.SubElement(root, 'mxCell', {
        'id': '15',
        'value': 'OAuth/API',
        'style': 'edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#39FF14;strokeWidth=2;fontColor=#39FF14;fontSize=12;',
        'edge': '1',
        'parent': '1',
        'source': '7',
        'target': '10'
    })
    ET.SubElement(cell, 'mxGeometry', {'relative': '1', 'as': 'geometry'})

    # Business to Firestore
    cell = ET.SubElement(root, 'mxCell', {
        'id': '16',
        'value': 'Read/Write',
        'style': 'edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#00BFFF;strokeWidth=2;fontColor=#00BFFF;fontSize=12;',
        'edge': '1',
        'parent': '1',
        'source': '6',
        'target': '8'
    })
    ET.SubElement(cell, 'mxGeometry', {'relative': '1', 'as': 'geometry'})

    # Client Layer to Vault
    cell = ET.SubElement(root, 'mxCell', {
        'id': '17',
        'value': 'Secrets',
        'style': 'edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#BF00FF;strokeWidth=2;fontColor=#BF00FF;fontSize=12;',
        'edge': '1',
        'parent': '1',
        'source': '7',
        'target': '9'
    })
    ET.SubElement(cell, 'mxGeometry', {'relative': '1', 'as': 'geometry'})

    return create_drawio_xml(graph_model)

def create_layered_architecture():
    """Create layered architecture diagram."""
    graph_model = ET.Element('mxGraphModel', {
        'dx': '1422',
        'dy': '794',
        'grid': '1',
        'gridSize': '10',
        'guides': '1',
        'tooltips': '1',
        'connect': '1',
        'arrows': '1',
        'fold': '1',
        'page': '1',
        'pageScale': '1',
        'pageWidth': '1169',
        'pageHeight': '827',
        'math': '0',
        'shadow': '0'
    })

    root = ET.SubElement(graph_model, 'root')
    ET.SubElement(root, 'mxCell', {'id': '0'})
    ET.SubElement(root, 'mxCell', {'id': '1', 'parent': '0'})

    # Title
    cell = ET.SubElement(root, 'mxCell', {
        'id': '2',
        'value': 'Strategiz Core - Layered Architecture',
        'style': 'text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;whiteSpace=wrap;rounded=0;fontSize=24;fontStyle=1;fontColor=#FFFFFF;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '340', 'y': '40', 'width': '500', 'height': '40', 'as': 'geometry'})

    # Layer 1: Service
    cell = ET.SubElement(root, 'mxCell', {
        'id': '3',
        'value': 'Layer 1: Service Layer (service-*)\nREST Controllers, API Endpoints, Request/Response Models',
        'style': 'rounded=0;whiteSpace=wrap;html=1;fillColor=#00BFFF;strokeColor=#FFFFFF;strokeWidth=3;fontSize=14;fontColor=#FFFFFF;fontStyle=1;align=left;verticalAlign=top;spacingLeft=10;spacingTop=10;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '120', 'y': '120', 'width': '940', 'height': '100', 'as': 'geometry'})

    # Service examples
    cell = ET.SubElement(root, 'mxCell', {
        'id': '4',
        'value': '• AuthenticationController\n• ProviderConnectionController\n• PortfolioController',
        'style': 'text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;whiteSpace=wrap;fontSize=12;fontColor=#FFFFFF;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '140', 'y': '160', 'width': '300', 'height': '50', 'as': 'geometry'})

    # Layer 2: Business
    cell = ET.SubElement(root, 'mxCell', {
        'id': '5',
        'value': 'Layer 2: Business Logic (business-*)\nCore Business Logic, Orchestration, Provider-Specific Services',
        'style': 'rounded=0;whiteSpace=wrap;html=1;fillColor=#39FF14;strokeColor=#1a1a1a;strokeWidth=3;fontSize=14;fontColor=#1a1a1a;fontStyle=1;align=left;verticalAlign=top;spacingLeft=10;spacingTop=10;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '120', 'y': '260', 'width': '940', 'height': '100', 'as': 'geometry'})

    # Business examples
    cell = ET.SubElement(root, 'mxCell', {
        'id': '6',
        'value': '• CoinbaseConnectionService\n• BinanceConnectionService\n• PortfolioAggregationService',
        'style': 'text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;whiteSpace=wrap;fontSize=12;fontColor=#1a1a1a;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '140', 'y': '300', 'width': '300', 'height': '50', 'as': 'geometry'})

    # Layer 3: Client
    cell = ET.SubElement(root, 'mxCell', {
        'id': '7',
        'value': 'Layer 3: Client Integration (client-*)\nExternal API Clients, OAuth Handlers, Data Transformation',
        'style': 'rounded=0;whiteSpace=wrap;html=1;fillColor=#BF00FF;strokeColor=#FFFFFF;strokeWidth=3;fontSize=14;fontColor=#FFFFFF;fontStyle=1;align=left;verticalAlign=top;spacingLeft=10;spacingTop=10;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '120', 'y': '400', 'width': '940', 'height': '100', 'as': 'geometry'})

    # Client examples
    cell = ET.SubElement(root, 'mxCell', {
        'id': '8',
        'value': '• CoinbaseClient\n• BinanceClient\n• OAuth2TokenClient',
        'style': 'text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;whiteSpace=wrap;fontSize=12;fontColor=#FFFFFF;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '140', 'y': '440', 'width': '300', 'height': '50', 'as': 'geometry'})

    # Layer 4: Data
    cell = ET.SubElement(root, 'mxCell', {
        'id': '9',
        'value': 'Layer 4: Data Access (data-*)\nRepositories, DAOs, Data Models, Persistence',
        'style': 'rounded=0;whiteSpace=wrap;html=1;fillColor=#00BFFF;strokeColor=#FFFFFF;strokeWidth=3;fontSize=14;fontColor=#FFFFFF;fontStyle=1;align=left;verticalAlign=top;spacingLeft=10;spacingTop=10;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '120', 'y': '540', 'width': '940', 'height': '100', 'as': 'geometry'})

    # Data examples
    cell = ET.SubElement(root, 'mxCell', {
        'id': '10',
        'value': '• FirestoreService\n• VaultService\n• UserRepository',
        'style': 'text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;whiteSpace=wrap;fontSize=12;fontColor=#FFFFFF;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '140', 'y': '580', 'width': '300', 'height': '50', 'as': 'geometry'})

    # External databases
    cell = ET.SubElement(root, 'mxCell', {
        'id': '11',
        'value': 'Firestore',
        'style': 'shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;backgroundOutline=1;size=15;fillColor=#1a1a1a;strokeColor=#39FF14;strokeWidth=2;fontSize=14;fontColor=#39FF14;fontStyle=1;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '180', 'y': '680', 'width': '120', 'height': '80', 'as': 'geometry'})

    cell = ET.SubElement(root, 'mxCell', {
        'id': '12',
        'value': 'Vault',
        'style': 'shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;backgroundOutline=1;size=15;fillColor=#1a1a1a;strokeColor=#BF00FF;strokeWidth=2;fontSize=14;fontColor=#BF00FF;fontStyle=1;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '530', 'y': '680', 'width': '120', 'height': '80', 'as': 'geometry'})

    # Arrows between layers
    for i, (source, target) in enumerate([('3', '5'), ('5', '7'), ('7', '9')]):
        cell = ET.SubElement(root, 'mxCell', {
            'id': f'arrow{i}',
            'value': '',
            'style': 'edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#FFFFFF;strokeWidth=3;',
            'edge': '1',
            'parent': '1',
            'source': source,
            'target': target
        })
        ET.SubElement(cell, 'mxGeometry', {'relative': '1', 'as': 'geometry'})

    return create_drawio_xml(graph_model)

def encode_diagram_for_png(xml_content):
    """Encode Draw.io XML for PNG embedding."""
    # Compress and encode the XML
    compressed = zlib.compress(xml_content.encode('utf-8'), 9)
    encoded = base64.b64encode(compressed).decode('ascii')
    return encoded

def create_png_with_diagram(xml_content, output_path, width=1200, height=800):
    """Create a PNG image with embedded Draw.io diagram."""
    # Create a basic image (will be replaced by actual diagram rendering)
    img = Image.new('RGB', (width, height), color='#1a1a1a')

    # Encode the diagram
    encoded = encode_diagram_for_png(xml_content)

    # Create PNG metadata
    meta = PngImagePlugin.PngInfo()
    meta.add_text('mxfile', xml_content)

    # Save with metadata
    img.save(output_path, 'PNG', pnginfo=meta)
    print(f"Created: {output_path}")

def create_provider_connection_flow():
    """Create provider connection flow diagram."""
    graph_model = ET.Element('mxGraphModel', {
        'dx': '1422',
        'dy': '900',
        'grid': '1',
        'gridSize': '10',
        'guides': '1',
        'tooltips': '1',
        'connect': '1',
        'arrows': '1',
        'fold': '1',
        'page': '1',
        'pageScale': '1',
        'pageWidth': '1400',
        'pageHeight': '900',
        'math': '0',
        'shadow': '0'
    })

    root = ET.SubElement(graph_model, 'root')
    ET.SubElement(root, 'mxCell', {'id': '0'})
    ET.SubElement(root, 'mxCell', {'id': '1', 'parent': '0'})

    # Title
    cell = ET.SubElement(root, 'mxCell', {
        'id': '2',
        'value': 'Provider Connection Flow (OAuth 2.0)',
        'style': 'text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;whiteSpace=wrap;rounded=0;fontSize=24;fontStyle=1;fontColor=#FFFFFF;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '400', 'y': '30', 'width': '600', 'height': '40', 'as': 'geometry'})

    # Swim lanes
    y_offset = 100
    actors = [
        ('User/Browser', '#00BFFF', 120),
        ('Strategiz UI', '#39FF14', 220),
        ('Strategiz Core', '#BF00FF', 320),
        ('Provider (Coinbase)', '#00BFFF', 420),
    ]

    for idx, (name, color, y) in enumerate(actors):
        cell = ET.SubElement(root, 'mxCell', {
            'id': f'lane{idx}',
            'value': name,
            'style': f'swimlane;html=1;startSize=30;fillColor={color};strokeColor=#FFFFFF;strokeWidth=2;fontSize=14;fontColor={"#FFFFFF" if color != "#39FF14" else "#1a1a1a"};fontStyle=1;',
            'vertex': '1',
            'parent': '1'
        })
        ET.SubElement(cell, 'mxGeometry', {'x': '80', 'y': str(y_offset + y), 'width': '1240', 'height': '100', 'as': 'geometry'})

    # Flow steps
    steps = [
        {'id': 's1', 'text': '1. Click "Connect\nCoinbase"', 'x': 150, 'y': 140, 'parent': 'lane0', 'color': '#00BFFF'},
        {'id': 's2', 'text': '2. Initiate OAuth', 'x': 350, 'y': 240, 'parent': 'lane1', 'color': '#39FF14'},
        {'id': 's3', 'text': '3. Request\nAuthorization URL', 'x': 550, 'y': 340, 'parent': 'lane2', 'color': '#BF00FF'},
        {'id': 's4', 'text': '4. Generate URL\n+ State', 'x': 750, 'y': 440, 'parent': 'lane3', 'color': '#00BFFF'},
        {'id': 's5', 'text': '5. Redirect to\nProvider', 'x': 950, 'y': 140, 'parent': 'lane0', 'color': '#00BFFF'},
        {'id': 's6', 'text': '6. User Approves', 'x': 950, 'y': 440, 'parent': 'lane3', 'color': '#00BFFF'},
        {'id': 's7', 'text': '7. Callback with\nAuth Code', 'x': 1150, 'y': 240, 'parent': 'lane1', 'color': '#39FF14'},
        {'id': 's8', 'text': '8. Exchange Code\nfor Token', 'x': 550, 'y': 540, 'parent': 'lane2', 'color': '#BF00FF'},
        {'id': 's9', 'text': '9. Store in Vault', 'x': 350, 'y': 540, 'parent': 'lane2', 'color': '#BF00FF'},
        {'id': 's10', 'text': '10. Fetch Portfolio', 'x': 150, 'y': 540, 'parent': 'lane2', 'color': '#BF00FF'},
    ]

    for step in steps:
        cell = ET.SubElement(root, 'mxCell', {
            'id': step['id'],
            'value': step['text'],
            'style': f'rounded=1;whiteSpace=wrap;html=1;fillColor={step["color"]};strokeColor=#FFFFFF;strokeWidth=2;fontSize=12;fontColor={"#FFFFFF" if step["color"] != "#39FF14" else "#1a1a1a"};fontStyle=1;',
            'vertex': '1',
            'parent': '1'
        })
        ET.SubElement(cell, 'mxGeometry', {'x': str(step['x']), 'y': str(step['y']), 'width': '140', 'height': '60', 'as': 'geometry'})

    # Add arrows between steps
    connections = [
        ('s1', 's2'), ('s2', 's3'), ('s3', 's4'), ('s4', 's5'),
        ('s5', 's6'), ('s6', 's7'), ('s7', 's8'), ('s8', 's9'), ('s9', 's10')
    ]

    for idx, (source, target) in enumerate(connections):
        cell = ET.SubElement(root, 'mxCell', {
            'id': f'arrow{idx}',
            'value': '',
            'style': 'edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#FFFFFF;strokeWidth=2;',
            'edge': '1',
            'parent': '1',
            'source': source,
            'target': target
        })
        ET.SubElement(cell, 'mxGeometry', {'relative': '1', 'as': 'geometry'})

    return create_drawio_xml(graph_model)

def create_authentication_flow():
    """Create authentication flow diagram."""
    graph_model = ET.Element('mxGraphModel', {
        'dx': '1422',
        'dy': '794',
        'grid': '1',
        'gridSize': '10',
        'guides': '1',
        'tooltips': '1',
        'connect': '1',
        'arrows': '1',
        'fold': '1',
        'page': '1',
        'pageScale': '1',
        'pageWidth': '1169',
        'pageHeight': '827',
        'math': '0',
        'shadow': '0'
    })

    root = ET.SubElement(graph_model, 'root')
    ET.SubElement(root, 'mxCell', {'id': '0'})
    ET.SubElement(root, 'mxCell', {'id': '1', 'parent': '0'})

    # Title
    cell = ET.SubElement(root, 'mxCell', {
        'id': '2',
        'value': 'Multi-Factor Authentication Flow',
        'style': 'text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;whiteSpace=wrap;rounded=0;fontSize=24;fontStyle=1;fontColor=#FFFFFF;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '340', 'y': '40', 'width': '500', 'height': '40', 'as': 'geometry'})

    # User entry point
    cell = ET.SubElement(root, 'mxCell', {
        'id': '3',
        'value': 'User Login',
        'style': 'rounded=1;whiteSpace=wrap;html=1;fillColor=#00BFFF;strokeColor=#FFFFFF;strokeWidth=2;fontSize=14;fontColor=#FFFFFF;fontStyle=1;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '520', 'y': '120', 'width': '140', 'height': '60', 'as': 'geometry'})

    # Authentication methods
    methods = [
        {'id': 'm1', 'text': 'Passkey\n(WebAuthn)', 'x': 200, 'y': 240, 'color': '#39FF14'},
        {'id': 'm2', 'text': 'TOTP\n(Google Auth)', 'x': 400, 'y': '240', 'color': '#39FF14'},
        {'id': 'm3', 'text': 'SMS OTP', 'x': 600, 'y': 240, 'color': '#39FF14'},
        {'id': 'm4', 'text': 'Social OAuth\n(Google)', 'x': 800, 'y': 240, 'color': '#39FF14'},
    ]

    for method in methods:
        cell = ET.SubElement(root, 'mxCell', {
            'id': method['id'],
            'value': method['text'],
            'style': f'rounded=1;whiteSpace=wrap;html=1;fillColor={method["color"]};strokeColor=#1a1a1a;strokeWidth=2;fontSize=12;fontColor=#1a1a1a;fontStyle=1;',
            'vertex': '1',
            'parent': '1'
        })
        ET.SubElement(cell, 'mxGeometry', {'x': str(method['x']), 'y': str(method['y']), 'width': '140', 'height': '60', 'as': 'geometry'})

        # Arrow from user login to method
        cell = ET.SubElement(root, 'mxCell', {
            'id': f'arrow_{method["id"]}',
            'value': '',
            'style': 'edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#FFFFFF;strokeWidth=2;',
            'edge': '1',
            'parent': '1',
            'source': '3',
            'target': method['id']
        })
        ET.SubElement(cell, 'mxGeometry', {'relative': '1', 'as': 'geometry'})

    # Verification step
    cell = ET.SubElement(root, 'mxCell', {
        'id': 'verify',
        'value': 'Verify Credentials',
        'style': 'rhombus;whiteSpace=wrap;html=1;fillColor=#BF00FF;strokeColor=#FFFFFF;strokeWidth=2;fontSize=14;fontColor=#FFFFFF;fontStyle=1;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '500', 'y': '360', 'width': '180', 'height': '80', 'as': 'geometry'})

    # Arrows from methods to verification
    for method in methods:
        cell = ET.SubElement(root, 'mxCell', {
            'id': f'to_verify_{method["id"]}',
            'value': '',
            'style': 'edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#FFFFFF;strokeWidth=2;',
            'edge': '1',
            'parent': '1',
            'source': method['id'],
            'target': 'verify'
        })
        ET.SubElement(cell, 'mxGeometry', {'relative': '1', 'as': 'geometry'})

    # Success path
    cell = ET.SubElement(root, 'mxCell', {
        'id': 'success',
        'value': 'Generate JWT\n+ Session',
        'style': 'rounded=1;whiteSpace=wrap;html=1;fillColor=#39FF14;strokeColor=#1a1a1a;strokeWidth=2;fontSize=14;fontColor=#1a1a1a;fontStyle=1;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '520', 'y': '500', 'width': '140', 'height': '60', 'as': 'geometry'})

    # Failure path
    cell = ET.SubElement(root, 'mxCell', {
        'id': 'failure',
        'value': 'Auth Failed\nRate Limit',
        'style': 'rounded=1;whiteSpace=wrap;html=1;fillColor=#FF0000;strokeColor=#FFFFFF;strokeWidth=2;fontSize=14;fontColor=#FFFFFF;fontStyle=1;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '780', 'y': '370', 'width': '140', 'height': '60', 'as': 'geometry'})

    # Arrows
    cell = ET.SubElement(root, 'mxCell', {
        'id': 'success_arrow',
        'value': 'Valid',
        'style': 'edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#39FF14;strokeWidth=2;fontColor=#39FF14;',
        'edge': '1',
        'parent': '1',
        'source': 'verify',
        'target': 'success'
    })
    ET.SubElement(cell, 'mxGeometry', {'relative': '1', 'as': 'geometry'})

    cell = ET.SubElement(root, 'mxCell', {
        'id': 'failure_arrow',
        'value': 'Invalid',
        'style': 'edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#FF0000;strokeWidth=2;fontColor=#FF0000;',
        'edge': '1',
        'parent': '1',
        'source': 'verify',
        'target': 'failure'
    })
    ET.SubElement(cell, 'mxGeometry', {'relative': '1', 'as': 'geometry'})

    # Final step
    cell = ET.SubElement(root, 'mxCell', {
        'id': 'dashboard',
        'value': 'Redirect to\nDashboard',
        'style': 'rounded=1;whiteSpace=wrap;html=1;fillColor=#00BFFF;strokeColor=#FFFFFF;strokeWidth=2;fontSize=14;fontColor=#FFFFFF;fontStyle=1;',
        'vertex': '1',
        'parent': '1'
    })
    ET.SubElement(cell, 'mxGeometry', {'x': '520', 'y': '620', 'width': '140', 'height': '60', 'as': 'geometry'})

    cell = ET.SubElement(root, 'mxCell', {
        'id': 'final_arrow',
        'value': '',
        'style': 'edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#FFFFFF;strokeWidth=2;',
        'edge': '1',
        'parent': '1',
        'source': 'success',
        'target': 'dashboard'
    })
    ET.SubElement(cell, 'mxGeometry', {'relative': '1', 'as': 'geometry'})

    return create_drawio_xml(graph_model)

# Generate diagrams
if __name__ == '__main__':
    print("Generating Strategiz Core diagrams...")

    diagrams = [
        ('Architecture Overview', create_architecture_overview, 'architecture-overview.drawio.png', 1200, 800),
        ('Layered Architecture', create_layered_architecture, 'layered-architecture.drawio.png', 1200, 900),
        ('Provider Connection Flow', create_provider_connection_flow, 'provider-connection-flow.drawio.png', 1400, 900),
        ('Authentication Flow', create_authentication_flow, 'authentication-flow.drawio.png', 1200, 760),
    ]

    base_path = '/Users/cuztomizer/Documents/GitHub/strategiz-core/docs/diagrams/'

    for name, func, filename, width, height in diagrams:
        print(f"  Creating {name}...")
        xml = func()
        create_png_with_diagram(xml, base_path + filename, width, height)

    print("\n✅ All Core diagrams generated successfully!")
    print("You can now open these .drawio.png files in Draw.io to edit them.")
