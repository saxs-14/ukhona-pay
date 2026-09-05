import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { X, FileText, Download, Calendar, Printer, CheckCircle2, AlertCircle } from "lucide-react";
import client from "../api/client";

export default function VendorStatementModal({ isOpen, onClose, vendor }) {
  const [period, setPeriod] = useState("30"); // "7" (1 Week), "30" (1 Month), "90" (3 Months)
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleGeneratePdf = async () => {
    setLoading(true);
    setError("");
    try {
      // Fetch transaction history
      const res = await client.get("/transactions/me");
      const allTxns = res.data || [];

      // Helper to parse dates reliably across formats (ISO strings, timestamps, array format)
      const parseTxnDate = (d) => {
        if (!d) return new Date(0);
        if (Array.isArray(d)) {
          return new Date(d[0], (d[1] || 1) - 1, d[2] || 1, d[3] || 0, d[4] || 0, d[5] || 0);
        }
        return new Date(d);
      };

      // Filter by selected period (days)
      const days = Number(period);
      const cutoffDate = new Date();
      cutoffDate.setDate(cutoffDate.getDate() - days);
      cutoffDate.setHours(0, 0, 0, 0); // Start of day for exact period inclusion

      const filteredTxns = allTxns.filter((t) => {
        const txnDate = parseTxnDate(t.createdAt);
        return txnDate >= cutoffDate;
      });

      // Calculate totals strictly for the filtered period
      let totalIncome = 0;
      let totalPayouts = 0;

      filteredTxns.forEach((t) => {
        const amt = Number(t.amount || 0);
        const isReceived = t.direction === "RECEIVED" || (t.receiverId && vendor?.userId && t.receiverId === vendor?.userId && !t.description?.includes("Bank Cashout"));
        
        if (isReceived) {
          totalIncome += amt;
        } else {
          totalPayouts += amt;
        }
      });

      const periodLabel = days === 7 ? "1 Week (Last 7 Days)" : days === 30 ? "1 Month (Last 30 Days)" : "3 Months (Last 90 Days)";
      const issueDate = new Date().toLocaleDateString("en-ZA", { year: "numeric", month: "long", day: "numeric" });
      const startDate = cutoffDate.toLocaleDateString("en-ZA", { year: "numeric", month: "short", day: "numeric" });
      const endDate = new Date().toLocaleDateString("en-ZA", { year: "numeric", month: "short", day: "numeric" });

      // Build print & downloadable A4 styled HTML PDF statement
      const printWindow = window.open("", "_blank");
      const filename = `UKHONA_PAY_Statement_${days}Days_${vendor?.businessName?.replace(/[^a-zA-Z0-9]/g, "_") || "Vendor"}.pdf`;

      const rowsHtml = filteredTxns.map((t) => {
        const isReceived = t.direction === "RECEIVED" || (t.receiverId && vendor?.userId && t.receiverId === vendor?.userId && !t.description?.includes("Bank Cashout"));
        const typeLabel = isReceived ? "INCOME PAYMENT" : (t.description?.includes("Bank Cashout") ? "BANK CASHOUT" : "OUTGOING TRANSFER");
        const partyLabel = t.description ? t.description : (isReceived ? t.senderName : `Paid to ${t.receiverName || t.vendorName || "Vendor"}`);
        const dateStr = parseTxnDate(t.createdAt).toLocaleString("en-ZA");

        return `
          <tr style="border-bottom: 1px solid #E5E7EB;">
            <td style="padding: 10px 12px; font-size: 12px; color: #374151;">${dateStr}</td>
            <td style="padding: 10px 12px; font-size: 12px; font-family: monospace; color: #4B5563;">${t.reference}</td>
            <td style="padding: 10px 12px; font-size: 11px; font-weight: 600; color: ${isReceived ? "#15803D" : "#C2410C"};">${typeLabel}</td>
            <td style="padding: 10px 12px; font-size: 12px; color: #1F2937;">${partyLabel}</td>
            <td style="padding: 10px 12px; font-size: 12px; font-weight: 700; text-align: right; color: ${isReceived ? "#15803D" : "#C2410C"};">
              ${isReceived ? "+" : "-"}R ${Number(t.amount).toFixed(2)}
            </td>
          </tr>
        `;
      }).join("");

      const statementHtml = `
        <!DOCTYPE html>
        <html>
        <head>
          <title>UKHONA PAY - Vendor Statement (${vendor?.businessName})</title>
          <meta charset="utf-8" />
          <script src="https://cdnjs.cloudflare.com/ajax/libs/html2pdf.js/0.10.1/html2pdf.bundle.min.js"></script>
          <style>
            @page { size: A4; margin: 12mm; }
            body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; color: #1F2937; margin: 0; padding: 0; background-color: #F8FAFC; }
            .no-print {
              background: #1E293B; color: #FFFFFF; padding: 12px 24px; display: flex; justify-content: space-between; align-items: center; position: sticky; top: 0; z-index: 1000; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
            }
            .btn {
              border: none; padding: 8px 16px; border-radius: 8px; font-weight: 600; font-size: 13px; cursor: pointer; display: inline-flex; align-items: center; gap: 6px; transition: all 0.2s;
            }
            .btn-primary { background: #C2410C; color: #FFFFFF; }
            .btn-primary:hover { background: #EA580C; }
            .btn-secondary { background: #334155; color: #F8FAFC; }
            .btn-secondary:hover { background: #475569; }
            .page-container { background: #FFFFFF; width: 210mm; min-height: 297mm; margin: 20px auto; padding: 20mm; box-sizing: border-box; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1); border-radius: 4px; }
            
            @media print {
              .no-print { display: none !important; }
              body { background: #FFFFFF; }
              .page-container { margin: 0; padding: 0; width: 100%; box-shadow: none; border-radius: 0; }
            }

            .header-bar { display: flex; justify-content: space-between; align-items: center; border-bottom: 3px solid #C2410C; padding-bottom: 15px; margin-bottom: 25px; }
            .logo-text { font-size: 24px; font-weight: 900; color: #C2410C; letter-spacing: -0.5px; }
            .logo-sub { font-size: 11px; color: #6B7280; font-weight: 500; }
            .title-badge { background: #FFF7ED; color: #C2410C; padding: 6px 14px; border-radius: 20px; font-weight: 700; font-size: 12px; border: 1px solid #FFEDD5; }
            .meta-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 25px; }
            .meta-box { background: #F9FAFB; border: 1px solid #E5E7EB; border-radius: 12px; padding: 14px; }
            .meta-title { font-size: 10px; font-weight: 700; text-transform: uppercase; color: #6B7280; margin-bottom: 6px; }
            .meta-value { font-size: 13px; font-weight: 600; color: #111827; }
            .summary-card { background: #2F8F4E; color: #FFFFFF; border-radius: 14px; padding: 18px; display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 15px; margin-bottom: 30px; }
            .summary-item { text-align: center; }
            .summary-lbl { font-size: 11px; color: #DCFCE7; text-transform: uppercase; margin-bottom: 4px; }
            .summary-val { font-size: 20px; font-weight: 800; }
            table { width: 100%; border-collapse: collapse; margin-bottom: 30px; }
            th { background: #F3F4F6; text-align: left; padding: 10px 12px; font-size: 11px; font-weight: 700; color: #4B5563; border-bottom: 2px solid #D1D5DB; }
            .footer { border-top: 1px solid #E5E7EB; padding-top: 15px; margin-top: 40px; text-align: center; font-size: 10px; color: #9CA3AF; }
          </style>
        </head>
        <body>
          <div class="no-print">
            <div style="font-weight: 600; font-size: 14px;">UKHONA PAY • Official Statement PDF Preview</div>
            <div style="display: flex; gap: 10px;">
              <button class="btn btn-primary" onclick="downloadAsPDF()">📥 Download PDF File</button>
              <button class="btn btn-secondary" onclick="window.print()">🖨️ Print / Save PDF</button>
            </div>
          </div>

          <div class="page-container" id="statement-container">
            <div class="header-bar">
              <div>
                <div class="logo-text">UKHONA PAY</div>
                <div class="logo-sub">Fintech Payment & Savings Platform • Mbombela, South Africa</div>
              </div>
              <div class="title-badge">OFFICIAL VENDOR STATEMENT</div>
            </div>

            <div class="meta-grid">
              <div class="meta-box">
                <div class="meta-title">Vendor Account Details</div>
                <div class="meta-value">${vendor?.businessName || "Registered Vendor"}</div>
                <div style="font-size: 12px; color: #4B5563; margin-top: 3px;">Operating Rank: <strong>${vendor?.locationName || "Mbombela Taxi Rank"}</strong></div>
                <div style="font-size: 11px; color: #6B7280; font-weight: 500; font-family: monospace; margin-top: 3px;">QR Code: ${vendor?.qrCode || "—"}</div>
              </div>
              <div class="meta-box">
                <div class="meta-title">Statement Period</div>
                <div class="meta-value">${periodLabel}</div>
                <div style="font-size: 12px; color: #4B5563; margin-top: 3px;">Date Range: ${startDate} – ${endDate}</div>
                <div style="font-size: 11px; color: #6B7280; margin-top: 3px;">Generated on: ${issueDate}</div>
              </div>
            </div>

            <div class="summary-card">
              <div class="summary-item">
                <div class="summary-lbl">Total Payments Received</div>
                <div class="summary-val">+R ${totalIncome.toFixed(2)}</div>
              </div>
              <div class="summary-item">
                <div class="summary-lbl">Total Payouts / Transfers</div>
                <div class="summary-val">-R ${totalPayouts.toFixed(2)}</div>
              </div>
              <div class="summary-item">
                <div class="summary-lbl">Net Statement Volume</div>
                <div class="summary-val">R ${(totalIncome - totalPayouts).toFixed(2)}</div>
              </div>
            </div>

            <h3 style="font-size: 14px; margin-bottom: 12px; color: #111827;">Itemized Transaction Ledger (${filteredTxns.length} records)</h3>

            <table>
              <thead>
                <tr>
                  <th>Date & Time</th>
                  <th>Reference</th>
                  <th>Type</th>
                  <th>Party / Description</th>
                  <th style="text-align: right;">Amount (ZAR)</th>
                </tr>
              </thead>
              <tbody>
                ${rowsHtml || `<tr><td colspan="5" style="text-align: center; padding: 20px; color: #6B7280;">No transactions found for this period.</td></tr>`}
              </tbody>
            </table>

            <div class="footer">
              Official Document generated by UKHONA PAY Platform • Partnered with ABSA Bank South Africa • Verified Ledger Entry
            </div>
          </div>

          <script>
            function downloadAsPDF() {
              const element = document.getElementById('statement-container');
              const opt = {
                margin:       [10, 10, 10, 10],
                filename:     '${filename}',
                image:        { type: 'jpeg', quality: 0.98 },
                html2canvas:  { scale: 2, useCORS: true },
                jsPDF:        { unit: 'mm', format: 'a4', orientation: 'portrait' }
              };
              if (window.html2pdf) {
                html2pdf().set(opt).from(element).save();
              } else {
                window.print();
              }
            }

            // Auto trigger PDF download after load
            window.onload = function() {
              setTimeout(function() {
                downloadAsPDF();
              }, 500);
            };
          </script>
        </body>
        </html>
      `;

      if (printWindow) {
        printWindow.document.write(statementHtml);
        printWindow.document.close();
      } else {
        // Fallback: If pop-up blocker blocked window.open, trigger direct HTML statement file download
        const blob = new Blob([statementHtml], { type: "text/html" });
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `UKHONA_PAY_Statement_${days}Days.html`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
      }
    } catch (err) {
      setError("Failed to fetch statement data. Please check connection.");
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-sand-900/60 p-4 backdrop-blur-sm">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.95 }}
          className="relative w-full max-w-sm rounded-3xl border border-sand-200 bg-white p-6 shadow-2xl"
        >
          <button
            onClick={onClose}
            className="absolute right-4 top-4 rounded-full p-2 text-sand-400 hover:bg-sand-100 hover:text-sand-700"
          >
            <X size={20} />
          </button>

          <div className="text-center">
            <div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-2xl bg-terracotta-50 text-terracotta-600">
              <FileText size={24} />
            </div>
            <h3 className="font-display text-lg text-sand-900">Download Account Statement</h3>
            <p className="text-xs text-sand-500">Download official PDF vendor statement for financial records or bank applications</p>
          </div>

          <div className="my-5 space-y-3">
            <label className="block text-xs font-medium text-sand-600">Select Statement Duration</label>
            
            <div className="grid grid-cols-3 gap-2">
              {[
                { value: "7", label: "1 Week", desc: "Last 7 Days" },
                { value: "30", label: "1 Month", desc: "Last 30 Days" },
                { value: "90", label: "3 Months", desc: "Last 90 Days" },
              ].map((item) => (
                <button
                  key={item.value}
                  type="button"
                  onClick={() => setPeriod(item.value)}
                  className={`rounded-2xl border p-3 text-center transition ${
                    period === item.value
                      ? "border-terracotta-600 bg-terracotta-50/60 ring-2 ring-terracotta-100"
                      : "border-sand-200 bg-white hover:bg-sand-50"
                  }`}
                >
                  <p className={`text-sm font-bold ${period === item.value ? "text-terracotta-700" : "text-sand-900"}`}>
                    {item.label}
                  </p>
                  <p className="text-[10px] text-sand-400">{item.desc}</p>
                </button>
              ))}
            </div>
          </div>

          {error && (
            <div className="mb-4 flex items-center gap-2 rounded-xl bg-rose-50 p-3 text-xs text-rose-700">
              <AlertCircle size={16} className="shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <div className="flex gap-2">
            <button
              type="button"
              onClick={onClose}
              className="w-1/3 rounded-xl border border-sand-300 py-3 text-sm font-semibold text-sand-700 hover:bg-sand-50"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={handleGeneratePdf}
              disabled={loading}
              className="flex w-2/3 items-center justify-center gap-2 rounded-xl bg-terracotta-600 py-3 text-sm font-semibold text-white transition hover:bg-terracotta-700 disabled:opacity-50"
            >
              <Download size={16} /> {loading ? "Downloading..." : "Download PDF"}
            </button>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}

