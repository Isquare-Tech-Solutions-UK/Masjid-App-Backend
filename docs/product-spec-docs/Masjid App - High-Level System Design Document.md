# 

# 

# 

#  Masjid App

*High-Level System Design Documentation*

**Isquaretechsolutions.co.uk**

**Document Information**

| Document Title | High-Level System Design Documentation |
| :---- | :---- |
| **Version** | 1.0 |
| **Date** | November 24, 2025 |
| **Status** | Final Draft |
| **Prepared By** | Syed Fardeen & Mohaideen (Product Team) |

**Document Revision History**

| Version | Date | Author | Description of Changes |
| ----- | ----- | ----- | ----- |
| 1.0 | November 18, 2025 | Syed Fardeen | Initial project planning & Design created |

# 

# 

# 

# 

# 

# 

# Index {#index}

[Index	3](#index)

[1\. ARCHITECTURE OVERVIEW	4](#1.-architecture-overview)

[1.1 Design Philosophy	4](#1.1-design-philosophy)

[1.2 Architectural Strategy (Single-Tenant)	5](#1.2-architectural-strategy-\(single-tenant\))

[1.3 High-Level System Diagram	5](#1.3-high-level-system-diagram)

[2\. TECHNOLOGY STACK	6](#2.-technology-stack)

[2.1 Client-Side Technologies	6](#2.1-client-side-technologies)

[2.2 Server-Side Technologies	7](#2.2-server-side-technologies)

[2.3 Data Persistence Layer	7](#2.3-data-persistence-layer)

[3\. INFRASTRUCTURE & HOSTING	7](#3.-infrastructure-&-hosting)

[3.1 AWS Cloud Infrastructure	7](#3.1-aws-cloud-infrastructure)

[3.2 Distribution Layer (Web & Mobile)	7](#3.2-distribution-layer-\(web-&-mobile\))

[3.3 Network & Security	8](#3.3-network-&-security)

[4\. DATA FLOW SPECIFICATIONS	8](#4.-data-flow-specifications)

[4.1 API Request Flow (Standard Operation)	8](#4.1-api-request-flow-\(standard-operation\))

[4.2 Web Dashboard Loading Flow	8](#4.2-web-dashboard-loading-flow)

[4.3 Notification & Event Flow	8](#4.3-notification-&-event-flow)

[5\. SECURITY & COMPLIANCE	9](#5.-security-&-compliance)

[5.1 Data Isolation	9](#5.1-data-isolation)

[5.2 Encryption Standards	9](#5.2-encryption-standards)

[5.3 Payment Compliance (Stripe)	9](#5.3-payment-compliance-\(stripe\))

# 

# **1\. ARCHITECTURE OVERVIEW** {#1.-architecture-overview}

### **1.1 Design Philosophy** {#1.1-design-philosophy}

The Masjid App Platform is designed as a White-Label, Single-Tenant Solution. This architectural decision addresses the critical need for data privacy, customized branding, and independent scaling for each client (Masjid).  
The system prioritizes:

* Isolation: Complete separation of data and compute resources per client.  
* Scalability: Ability to handle high traffic spikes (e.g., Jumu'ah, Ramadan) without affecting other clients.  
* Security: Enterprise-grade encryption and network segregation.

## **1.2 Architectural Strategy (Single-Tenant)** {#1.2-architectural-strategy-(single-tenant)}

Unlike traditional multi-tenant architectures where all clients share a single database, this platform provides a dedicated infrastructure "stamp" for each Masjid.  
Key Characteristics:

* Dedicated Database: Each Masjid receives its own PostgreSQL RDS instance.  
* Dedicated Storage: Media assets are stored in isolated S3 buckets.  
* Branded Distribution: Each Masjid receives a unique Mobile App entry in the App Store/Play Store and a custom Admin Dashboard URL.

## **1.3 High-Level System Diagram** {#1.3-high-level-system-diagram}

The following diagram illustrates the interaction between the End Users, the Distribution Layer, and the AWS Secure Cloud Environment.

\[**Note** : Refer the Architecture Diagram Below\]  
![][image1]  
Figure 1: High-Level System Architecture demonstrating Single-Tenant isolation.  
---

# **2\. TECHNOLOGY STACK** {#2.-technology-stack}

## **2.1 Client-Side Technologies** {#2.1-client-side-technologies}

Member Mobile Application:

* Framework: Flutter 3.16 (Dart 3.2)  
* Distribution: Native Binary generation (IPA/APK) via Flutter Flavors.  
* State Management: Riverpod 2.4.x  
* Key Capability: "Write Once, Deploy Everywhere" allows a single codebase to generate unique, branded apps for infinite Masjids.

Admin Web Application:

* Framework: React 18  
* Styling: TailwindCSS 3.x  
* Build Tool: Vite 5.x  
* Hosting: Serverless Static Hosting (S3 \+ CloudFront).

## **2.2 Server-Side Technologies** {#2.2-server-side-technologies}

Backend API:

* Language: Java 21 LTS  
* Framework: Spring Boot 3.2  
* Deployment: Dockerized containers managed by AWS Elastic Beanstalk.  
* Responsibilities: Prayer time calculation, CSV parsing, notification scheduling, and payment orchestration.

## **2.3 Data Persistence Layer** {#2.3-data-persistence-layer}

Relational Database:

* Service: Amazon RDS (Relational Database Service)  
* Engine: PostgreSQL 15  
* Function: Stores structured data including User Profiles, Donation History, Event Schedules, and Configuration Settings.

Object Storage:

* Service: Amazon S3 (Standard Class)  
* Function: Stores unstructured media including Event Flyers, Masjid Logos, and raw CSV Schedule files.

---

# **3\. INFRASTRUCTURE & HOSTING** {#3.-infrastructure-&-hosting}

All infrastructure is deployed on Amazon Web Services (AWS) using a secure VPC configuration.

## **3.1 AWS Cloud Infrastructure** {#3.1-aws-cloud-infrastructure}

The backend operates within a private virtual network to ensure security.

* Compute: AWS Elastic Beanstalk (Running Tomcat/Java Platform).  
* Load Balancing: AWS Application Load Balancer (ALB) handles traffic distribution and SSL termination.  
* DNS: Amazon Route 53 manages custom domains (e.g., api.masjid-uk.org).

## **3.2 Distribution Layer (Web & Mobile)** {#3.2-distribution-layer-(web-&-mobile)}

To ensure high performance and low latency, the frontend logic is decoupled from the backend server.

* Web Distribution: The React Admin Panel is not served by the Java server. Instead, it is hosted on Amazon S3 and distributed via Amazon CloudFront (CDN). This ensures the dashboard loads instantly from edge locations worldwide.  
* Mobile Distribution: Mobile apps are published directly to Apple and Google stores. The app communicates strictly via HTTPS API calls to the backend.

## **3.3 Network & Security** {#3.3-network-&-security}

* SSL/TLS: Managed via AWS Certificate Manager (ACM). All traffic from Mobile Apps and Web Dashboards is encrypted in transit.  
* Private Subnets: The RDS Database and Spring Boot Backend reside in private subnets, inaccessible directly from the public internet. Access is granted only via the Load Balancer.

---

# **4\. DATA FLOW SPECIFICATIONS** {#4.-data-flow-specifications}

## **4.1 API Request Flow (Standard Operation)** {#4.1-api-request-flow-(standard-operation)}

When a user opens the app to check prayer times:

1. Request: Mobile App sends HTTPS GET request to api.\[masjid-name\].com.  
2. Routing: Route 53 resolves DNS to the Load Balancer.  
3. Entry: Application Load Balancer (ALB) decrypts SSL and performs health check.  
4. Processing: Request forwarded to Spring Boot (Elastic Beanstalk).  
5. Query: Spring Boot queries RDS (Prayer Times) or computes countdown logic.  
6. Response: JSON payload returned to the device.

## **4.2 Web Dashboard Loading Flow** {#4.2-web-dashboard-loading-flow}

When an admin logs in to manage the masjid:

1. Asset Fetch: Browser requests admin.\[masjid-name\].com.  
2. Delivery: Amazon CloudFront serves the cached React static files (HTML/JS/CSS).  
3. Authentication: React App initiates an API handshake with the Backend to validate Admin credentials via JWT (JSON Web Token).

## **4.3 Notification & Event Flow** {#4.3-notification-&-event-flow}

When an event is published:

1. Trigger: Admin clicks "Publish" on the dashboard.  
2. Upload: Event image is uploaded to S3; URL is stored in RDS.  
3. Broadcast: Spring Boot triggers the Firebase Cloud Messaging (FCM) API.  
4. Delivery: FCM pushes the notification to all devices subscribed to that specific Masjid's topic.

---

# **5\. SECURITY & COMPLIANCE** {#5.-security-&-compliance}

## **5.1 Data Isolation** {#5.1-data-isolation}

The Single-Tenant architecture ensures strict data sovereignty.

* Database Level: Physical separation of database instances.  
* Application Level: Isolated runtime environments.  
* Benefit: Eliminates the risk of "Noisy Neighbor" performance issues and cross-tenant data leakage.

## **5.2 Encryption Standards** {#5.2-encryption-standards}

* In Transit: TLS 1.2+ is enforced for all API communication.  
* At Rest: AWS KMS (Key Management Service) is used to encrypt RDS volumes and S3 buckets.

## **5.3 Payment Compliance (Stripe)** {#5.3-payment-compliance-(stripe)}

The application utilizes Stripe Connect to handle payments.

* No Sensitive Data: The backend never touches or stores Credit Card numbers (PAN) or CVV codes.  
* Tokenization: The client app exchanges payment details directly with Stripe for a secure token.  
* Webhooks: Stripe notifies the backend server asynchronously upon successful payment capture.

[image1]: <data:image/png;base64,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>