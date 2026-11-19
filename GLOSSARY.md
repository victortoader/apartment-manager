# Apartment Manager

Managing rented apartments: their tenants, billing, tickets and handovers, as seen by the owner, admins and tenants.

## People and access

**Owner**:
The person who owns the apartments and has full control over everything in the portal.

**Admin**:
A person who manages every apartment on the owner's behalf, but cannot delete.

**Tenant**:
A person renting an apartment, who can only see and act on their assigned apartment.
_Avoid_: renter, user (when you mean a tenant specifically)

**Assigned apartment**:
The one apartment a tenant currently belongs to. It alone decides what the tenant can see; a tenant keeps no access to apartments they were assigned to before.
_Avoid_: own apartment, home apartment

## Billing

**Billing document**:
A file uploaded to an apartment's billing records, with a document type and an amount.
_Avoid_: BillPayment, paid bill

**Bill**:
A billing document that asks for payment.
_Avoid_: invoice

**Proof of payment** (short: proof):
A billing document showing that money was paid.
_Avoid_: bill proof, receipt

## Apartment records

**Apartment document**:
A file attached to an apartment's document list, of one kind: handover protocol, bills, photos or other. An apartment document of kind "bills" is not a billing document: it has no amount and is not part of the billing records.
_Avoid_: protocol (when meaning any document)

**Handover protocol**:
The signed record of an apartment's condition when a tenant moves in or out, either uploaded or generated from an inspection.
_Avoid_: Übergabeprotokoll, inspection PDF
