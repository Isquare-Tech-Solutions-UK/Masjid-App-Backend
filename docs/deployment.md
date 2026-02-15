# CI/CD Deployment Guide

This guide explains how the CI/CD pipeline for the Masjid App Backend is configured and how to set it up.

## Overview

The pipeline uses GitHub Actions to build Docker images and deploy them to EC2 instances using Docker Compose.

- **Development Environment (`dev`)**: Triggered by pushes to `dev` or `main`.
- **Production Environment (`prod`)**: Triggered by publishing a Release.

## Prerequisites

### 1. GitHub Secrets

You must configure the following secrets in your GitHub repository (**Settings > Secrets and variables > Actions**).

#### Global Secrets (Used by both environments)
| Secret Name | Description |
|---|---|
| `DOCKER_USERNAME` | Your Docker Hub username. |
| `DOCKER_PASSWORD` | Your Docker Hub password or access token. |

#### Development Environment Secrets
| Secret Name | Description |
|---|---|
| `DEV_EC2_HOST` | Public IP or DNS of the Dev EC2 instance. |
| `DEV_EC2_USERNAME` | SSH username (e.g., `ubuntu`, `ec2-user`). |
| `DEV_EC2_SSH_KEY` | Content of the Private SSH Key (`.pem` file). |
| `DEV_DB_USERNAME` | Database username for the Dev environment. |
| `DEV_DB_PASSWORD` | Database password for the Dev environment. |

#### Production Environment Secrets
| Secret Name | Description |
|---|---|
| `PROD_EC2_HOST` | Public IP or DNS of the Prod EC2 instance. |
| `PROD_EC2_USERNAME` | SSH username. |
| `PROD_EC2_SSH_KEY` | Content of the Private SSH Key. |
| `PROD_DB_USERNAME` | Database username for Production. |
| `PROD_DB_PASSWORD` | Database password for Production. |

### 2. EC2 Setup

Ensure your EC2 instances have:
- **Docker** and **Docker Compose** installed.
- **Ports Configured**:
    - **Dev**: Allow inbound traffic on port `8080` (or `80` if using Nginx reverse proxy).
    - **Prod**: Allow inbound traffic on port `8080`.

## Deployment Process

1.  **Code Changes**: Push code to `dev` or `main`.
2.  **Build**: GitHub Actions builds the Docker image and pushes it to Docker Hub with tag `dev-latest`.
3.  **Deploy**: GitHub Actions connects to EC2, copies `docker-compose.dev.yml`, and restarts the application.
4.  **Verification**: The application exposes a health check endpoint at `/api/v1/actuator/health`.

## Directory Structure on EC2

The deployment script creates and uses the `~/masjid-app` directory on your EC2 instance.
