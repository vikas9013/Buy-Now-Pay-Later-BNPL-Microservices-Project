# BNPL Microservices and Real Payment Services Test Report

**Execution Timestamp:** 2026-10-10 12:32:54  
**Environment:** Spring Cloud Microservices + PostgreSQL 18 + Redis 7 + Apache Kafka + Redisson RLock  
**API Gateway Entrypoint:** http://localhost:8080  
**Payment Gateways Integrated:** Razorpay (HMAC-SHA256 UPI) and Stripe (3D-Secure Cards)

---

## Test Execution Summary

| Test Case | Customer | Action Executed | Outcome / Metric | Status |
| :--- | :--- | :--- | :--- | :--- |
| Customer Onboarding | Aarav Sharma | Account Created and KYC Approved | Initial Limit: Rs 10000 | PASSED |
| Customer Onboarding | Priya Patel | Account Created and KYC Approved | Initial Limit: Rs 10000 | PASSED |
| Customer Onboarding | Rohan Gupta | Account Created and KYC Approved | Initial Limit: Rs 10000 | PASSED |
| Customer Onboarding | Ananya Iyer | Account Created and KYC Approved | Initial Limit: Rs 10000 | PASSED |
| BNPL Checkout (1 Month EMI) | Aarav Sharma | Purchase at Swiggy Gourmet | Rs 850 @ 0% Int -> Rs 850/mo | PASSED |
| BNPL Checkout (3 Month EMI) | Priya Patel | Purchase at Myntra Fashion | Rs 3000 @ 5% Int -> Rs 1050/mo | PASSED |
| BNPL Checkout (6 Month EMI) | Rohan Gupta | Purchase at Amazon Electronics | Rs 6000 @ 10% Int -> Rs 1100/mo | PASSED |
| BNPL Checkout (9 Month EMI) | Ananya Iyer | Purchase at Croma Digital Store | Rs 5000 @ 12% Int -> Rs 622.22/mo | PASSED |
| BNPL Checkout (12 Month EMI) | Aarav Sharma | Purchase at Apple Store India | Rs 4000 @ 15% Int -> Rs 383.33/mo | PASSED |
| Razorpay UPI Repayment | Priya Patel | Verified and Paid Rs 1050 | Payment ID: pay_Rzp66268535, Method: UPI | PASSED |
| Stripe Card Repayment | Rohan Gupta | Verified and Paid Rs 1100 | Payment ID: ch_Stripe61415614, Method: CREDIT_CARD | PASSED |
| Redis Idempotency Engine | Aarav Sharma | Duplicate Request Protection | Exact match, 0 double charges, cache latency 25ms | PASSED |
| Credit Limit Enforcement | Aarav Sharma | Rs 50,000 Over-Limit Purchase | Rejected: Insufficient credit limit | PASSED |
| Redisson Distributed Locking | Ananya Iyer | 4 Parallel Checkouts | 4 of 4 succeeded, exact credit balance preserved | PASSED |

---

## Real Retail Purchases and EMI Schedule Matrix

| Customer | Merchant | Principal | Tenure | Total Due (@ Int) | Monthly Installment |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Aarav Sharma | Swiggy Gourmet | Rs 850 | 1 Month(s) | Rs 850 | Rs 850 |
| Priya Patel | Myntra Fashion | Rs 3000 | 3 Month(s) | Rs 3150 | Rs 1050 |
| Rohan Gupta | Amazon Electronics | Rs 6000 | 6 Month(s) | Rs 6600 | Rs 1100 |
| Ananya Iyer | Croma Digital Store | Rs 5000 | 9 Month(s) | Rs 5600 | Rs 622.22 |
| Aarav Sharma | Apple Store India | Rs 4000 | 12 Month(s) | Rs 4600 | Rs 383.33 |

---

## Enterprise Transaction Safety Metrics

1. **Idempotency Engine (X-Idempotency-Key):**
   - Cache hit detected and served duplicate requests within **25ms**.
   - Guaranteed **zero double-charging** across client retries.

2. **Distributed Concurrency Lock (Redisson RLock):**
   - Successfully synchronized multiple concurrent threads during parallel checkout.
   - Preserved exact mathematical integrity of customer credit limits.

3. **Multi-Gateway Repayments:**
   - **Razorpay UPI:** Authenticated via cryptographic HMAC-SHA256 signature verification.
   - **Stripe Card:** Authorized and verified PaymentIntent token.
   - Real-time customer credit limit restoration via inter-service REST communication.

4. **Event-Driven Messaging (Apache Kafka):**
   - Published events to topics: 'bnpl-checkout-events' and 'bnpl-repayment-events'.
   - Notification service processed events for asynchronous customer SMS dispatch.

---
**Report generated automatically by BNPL Automated Test Harness.**
