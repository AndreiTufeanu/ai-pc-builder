from datetime import datetime
from typing import Dict, Any
from models import ComponentData
import constants

def create_component_document(component: ComponentData) -> str:
    """Create a searchable document string based on component type"""
    specs = component.specifications or {}
    component_type = component.type.upper()

    doc_parts = [
        f"[ID: {component.id}]",
        f"Component: {component.name}",
        f"Type: {component.type}",
        f"Price: ${component.price or 'N/A'}"
    ]

    # Add manufacturer and model if available
    manufacturer = specs.get('manufacturer', '')
    model = specs.get('model', '')
    if manufacturer or model:
        doc_parts.append(f"Brand: {manufacturer} {model}".strip())

    # Add type-specific specifications
    if component_type in constants.COMPONENT_SPECS_MAP:
        for field_name in constants.COMPONENT_SPECS_MAP[component_type]:
            if field_name in ['manufacturer', 'model']:  # Already added above
                continue

            value = specs.get(field_name)
            if value is not None:
                doc_parts.append(f"{field_name}: {value}")

    return "\n".join(doc_parts)


def create_component_metadata(component: ComponentData) -> Dict[str, Any]:
    """Create type-specific metadata for ChromaDB filtering"""
    specs = component.specifications or {}
    component_type = component.type.upper()

    # Base metadata
    metadata = {
        "component_id": component.id,
        "name": component.name,
        "component_type": component_type,
        "price": component.price or 0.0,
        "source": "database",
        "updated_at": datetime.now().isoformat()
    }

    # Add type-specific fields
    if component_type in constants.COMPONENT_SPECS_MAP:
        for field_name in constants.COMPONENT_SPECS_MAP[component_type]:
            value = specs.get(field_name)
            if value is not None:
                if isinstance(value, list):
                    metadata[field_name] = ", ".join(str(item) for item in value)
                else:
                    metadata[field_name] = value

    return metadata