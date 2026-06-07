import { describe, it, expect } from "vitest";

describe("Smoke tests", () => {
  it("true is true", () => {
    expect(true).toBe(true);
  });

  it("api module exports key functions", async () => {
    const api = await import("@/lib/api");
    expect(api.getProducts).toBeDefined();
    expect(api.getDashboard).toBeDefined();
    expect(api.getWarehouses).toBeDefined();
    expect(api.getSuppliers).toBeDefined();
    expect(api.getCustomers).toBeDefined();
    expect(api.getOrders).toBeDefined();
    expect(api.getInvoices).toBeDefined();
    expect(api.getNotifications).toBeDefined();
    expect(api.getSales).toBeDefined();
    expect(api.getMargins).toBeDefined();
    expect(api.getProduct).toBeDefined();
    expect(api.getInvoiceItems).toBeDefined();
    expect(api.getOrderItems).toBeDefined();
    expect(api.getActiveSession).toBeDefined();
    expect(api.getMainAccount).toBeDefined();
    expect(api.createProduct).toBeDefined();
    expect(api.createCustomer).toBeDefined();
    expect(api.createOrder).toBeDefined();
    expect(api.createInvoice).toBeDefined();
    expect(api.createSupplier).toBeDefined();
    expect(api.createWarehouse).toBeDefined();
    expect(api.updateWarehouse).toBeDefined();
    expect(api.deleteWarehouse).toBeDefined();
    expect(api.getProductsForSale).toBeDefined();
    expect(api.getProductByBarcode).toBeDefined();
  });
});
