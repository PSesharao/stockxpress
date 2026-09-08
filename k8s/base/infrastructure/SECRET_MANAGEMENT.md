# Secret Management for StockXpress Kubernetes Deployment

## ⚠️ CRITICAL: Never Commit Secrets to Git!

This directory demonstrates **three production-ready approaches** for managing secrets in Kubernetes without committing sensitive data to version control.

---

## Quick Start: Which Approach Should I Use?

| Approach | Best For | Complexity | Security |
|----------|----------|------------|----------|
| **Manual kubectl** | Development/Testing | Low | Medium |
| **Sealed Secrets** | GitOps workflows | Medium | High |
| **External Secrets Operator** | Production with cloud | Medium-High | Very High |

---

## Approach 1: Manual Secret Creation (Development)

### ✅ Pros:
- Simple, no additional tools
- Works everywhere
- Good for local development

### ❌ Cons:
- Manual process
- No GitOps support
- Secrets not versioned

### How to Use:

#### Step 1: Create Secret Locally (Never Commit!)

```bash
# Create mysql-secrets.yml from template
cp mysql-secrets.yml.template mysql-secrets.yml

# Edit with actual values
vim mysql-secrets.yml

# OR create directly with kubectl
kubectl create secret generic mysql-order-secret \
  --from-literal=root-password='your-strong-root-password' \
  --from-literal=username='orderuser' \
  --from-literal=password='your-strong-user-password' \
  --namespace=stockxpress

kubectl create secret generic mysql-inventory-secret \
  --from-literal=root-password='your-strong-root-password' \
  --from-literal=username='inventoryuser' \
  --from-literal=password='your-strong-user-password' \
  --namespace=stockxpress
```

#### Step 2: Verify Secrets

```bash
# List secrets
kubectl get secrets -n stockxpress

# Describe secret (doesn't show values)
kubectl describe secret mysql-order-secret -n stockxpress

# View secret values (base64 encoded)
kubectl get secret mysql-order-secret -n stockxpress -o yaml

# Decode specific value
kubectl get secret mysql-order-secret -n stockxpress -o jsonpath='{.data.password}' | base64 -d
```

#### Step 3: Ensure .gitignore Excludes Secrets

```bash
# Verify mysql-secrets.yml is ignored
cat .gitignore | grep mysql-secrets.yml

# If not present, add it
echo 'k8s/base/infrastructure/mysql-secrets.yml' >> .gitignore
```

---

## Approach 2: Sealed Secrets (GitOps-Friendly)

### ✅ Pros:
- Secrets can be committed to Git (encrypted)
- Full GitOps support
- Audit trail in Git history
- Automatic decryption in cluster

### ❌ Cons:
- Requires SealedSecrets controller
- Cluster-specific encryption keys
- Re-encryption needed for key rotation

### Installation:

```bash
# Install Sealed Secrets controller
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.0/controller.yaml

# Install kubeseal CLI
# macOS:
brew install kubeseal

# Linux:
wget https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.0/kubeseal-linux-amd64 -O kubeseal
chmod +x kubeseal
sudo mv kubeseal /usr/local/bin/

# Windows:
choco install kubeseal
```

### Usage:

#### Step 1: Create Plain Secret (Temporary)

```bash
# Create temporary plain secret file
cat <<EOF > mysql-secrets-plain.yml
apiVersion: v1
kind: Secret
metadata:
  name: mysql-order-secret
  namespace: stockxpress
type: Opaque
stringData:
  root-password: "MySecureRootPassword123!"
  username: "orderuser"
  password: "MySecureUserPassword456!"
EOF
```

#### Step 2: Seal the Secret

```bash
# Encrypt secret
kubeseal -f mysql-secrets-plain.yml -w mysql-order-sealed-secret.yml

# Result: mysql-order-sealed-secret.yml contains encrypted data (safe to commit)
cat mysql-order-sealed-secret.yml
```

#### Step 3: Commit Sealed Secret

```bash
# Delete plain secret
rm mysql-secrets-plain.yml

# Commit encrypted version
git add mysql-order-sealed-secret.yml
git commit -m "Add sealed MySQL order secret"
git push
```

#### Step 4: Apply to Cluster

```bash
# Apply sealed secret
kubectl apply -f mysql-order-sealed-secret.yml

# Controller automatically decrypts and creates real Secret
kubectl get secret mysql-order-secret -n stockxpress
```

### Reference:
- See `sealed-secrets-example.yml` for complete example
- Official docs: https://sealed-secrets.netlify.app/

---

## Approach 3: External Secrets Operator (Production)

### ✅ Pros:
- Centralized secret management
- Integration with cloud providers
- Automatic rotation support
- Fine-grained access control
- Audit logging

### ❌ Cons:
- Requires external secret store
- Additional infrastructure
- Cloud-specific configuration

### Supported Backends:
- AWS Secrets Manager
- Google Cloud Secret Manager
- Azure Key Vault
- HashiCorp Vault
- Kubernetes Secrets (development)

### Installation:

```bash
# Add Helm repo
helm repo add external-secrets https://charts.external-secrets.io
helm repo update

# Install External Secrets Operator
helm install external-secrets \
  external-secrets/external-secrets \
  -n external-secrets-system \
  --create-namespace

# Verify installation
kubectl get pods -n external-secrets-system
```

### AWS Secrets Manager Example:

#### Step 1: Create Secrets in AWS

```bash
# Create secrets in AWS Secrets Manager
aws secretsmanager create-secret \
  --name stockxpress/mysql-order/root-password \
  --secret-string "MySecureRootPassword123!" \
  --region us-east-1

aws secretsmanager create-secret \
  --name stockxpress/mysql-order/username \
  --secret-string "orderuser" \
  --region us-east-1

aws secretsmanager create-secret \
  --name stockxpress/mysql-order/password \
  --secret-string "MySecureUserPassword456!" \
  --region us-east-1
```

#### Step 2: Configure IAM (EKS with IRSA)

```bash
# Create IAM policy
cat <<EOF > secrets-policy.json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue",
        "secretsmanager:DescribeSecret"
      ],
      "Resource": "arn:aws:secretsmanager:us-east-1:*:secret:stockxpress/*"
    }
  ]
}
EOF

aws iam create-policy \
  --policy-name StockXpressSecretsPolicy \
  --policy-document file://secrets-policy.json

# Create service account with IAM role
eksctl create iamserviceaccount \
  --name stockxpress-sa \
  --namespace stockxpress \
  --cluster your-cluster-name \
  --attach-policy-arn arn:aws:iam::YOUR_ACCOUNT:policy/StockXpressSecretsPolicy \
  --approve
```

#### Step 3: Apply External Secrets

```bash
# Apply SecretStore and ExternalSecret
kubectl apply -f external-secrets-operator.yml

# Verify ExternalSecret is synced
kubectl get externalsecrets -n stockxpress
kubectl describe externalsecret mysql-order-external-secret -n stockxpress

# Verify Secret was created
kubectl get secret mysql-order-secret -n stockxpress
```

### Reference:
- See `external-secrets-operator.yml` for complete example
- Official docs: https://external-secrets.io/

---

## CI/CD Integration

### GitHub Actions with Sealed Secrets

```yaml
name: Deploy with Sealed Secrets

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup kubectl
        uses: azure/setup-kubectl@v3
      
      - name: Apply sealed secrets
        run: |
          kubectl apply -f k8s/base/infrastructure/mysql-order-sealed-secret.yml
          kubectl apply -f k8s/base/infrastructure/mysql-inventory-sealed-secret.yml
      
      - name: Wait for secrets to be unsealed
        run: |
          kubectl wait --for=condition=Synced \
            sealedsecret/mysql-order-secret \
            -n stockxpress \
            --timeout=60s
```

### GitHub Actions with External Secrets

```yaml
name: Deploy with External Secrets

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    permissions:
      id-token: write  # For OIDC
      contents: read
    steps:
      - uses: actions/checkout@v3
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v2
        with:
          role-to-assume: arn:aws:iam::YOUR_ACCOUNT:role/GitHubActionsRole
          aws-region: us-east-1
      
      - name: Apply External Secrets
        run: |
          kubectl apply -f k8s/base/infrastructure/external-secrets-operator.yml
      
      - name: Wait for secrets to sync
        run: |
          kubectl wait --for=condition=SecretSynced \
            externalsecret/mysql-order-external-secret \
            -n stockxpress \
            --timeout=120s
```

---

## Rotation Strategy

### Manual Rotation

```bash
# Update secret in AWS
aws secretsmanager update-secret \
  --secret-id stockxpress/mysql-order/password \
  --secret-string "NewSecurePassword789!"

# ExternalSecret will auto-sync within refreshInterval (default: 1h)
# Or force immediate sync:
kubectl annotate externalsecret mysql-order-external-secret \
  force-sync=$(date +%s) \
  -n stockxpress

# Restart pods to use new secret
kubectl rollout restart statefulset mysql-order -n stockxpress
```

### Automated Rotation (AWS Secrets Manager)

```bash
# Enable automatic rotation (Lambda required)
aws secretsmanager rotate-secret \
  --secret-id stockxpress/mysql-order/password \
  --rotation-lambda-arn arn:aws:lambda:us-east-1:YOUR_ACCOUNT:function:SecretsManagerRotation \
  --rotation-rules AutomaticallyAfterDays=30
```

---

## Security Best Practices

### 1. Never Commit Plaintext Secrets

```bash
# Always verify before committing
git add -A
git status

# Scan for secrets (install gitleaks)
gitleaks detect --source . --verbose

# If secret was committed, remove it:
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch k8s/base/infrastructure/mysql-secrets.yml" \
  --prune-empty --tag-name-filter cat -- --all
```

### 2. Use Strong Passwords

```bash
# Generate strong passwords
openssl rand -base64 32

# Or use password manager
1Password, LastPass, Bitwarden, etc.
```

### 3. Principle of Least Privilege

```yaml
# Grant minimal permissions
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: secret-reader
  namespace: stockxpress
rules:
  - apiGroups: [""]
    resources: ["secrets"]
    resourceNames: ["mysql-order-secret", "mysql-inventory-secret"]
    verbs: ["get"]
```

### 4. Enable Audit Logging

```bash
# Monitor secret access
kubectl get events -n stockxpress --field-selector involvedObject.kind=Secret

# AWS CloudTrail for Secrets Manager
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=ResourceName,AttributeValue=stockxpress/mysql-order/password
```

### 5. Encrypt Secrets at Rest

```yaml
# Enable encryption at rest (EKS example)
apiVersion: v1
kind: EncryptionConfiguration
resources:
  - resources:
      - secrets
    providers:
      - aescbc:
          keys:
            - name: key1
              secret: <BASE64_ENCODED_SECRET>
      - identity: {}
```

---

## Troubleshooting

### Secret Not Found

```bash
# Check if secret exists
kubectl get secret mysql-order-secret -n stockxpress

# Check if namespace exists
kubectl get namespace stockxpress

# Create namespace if missing
kubectl create namespace stockxpress
```

### ExternalSecret Not Syncing

```bash
# Check ExternalSecret status
kubectl describe externalsecret mysql-order-external-secret -n stockxpress

# Check operator logs
kubectl logs -n external-secrets-system deployment/external-secrets

# Verify IAM permissions
aws sts get-caller-identity
aws secretsmanager get-secret-value --secret-id stockxpress/mysql-order/password
```

### SealedSecret Decryption Failed

```bash
# Check SealedSecret controller logs
kubectl logs -n kube-system deployment/sealed-secrets-controller

# Verify certificate
kubeseal --fetch-cert

# Re-seal with correct certificate
kubeseal -f mysql-secrets-plain.yml -w mysql-order-sealed-secret.yml
```

---

## Migration Guide

### From Committed Secrets to Sealed Secrets

```bash
# 1. Install SealedSecrets controller
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.0/controller.yaml

# 2. Get existing secret
kubectl get secret mysql-order-secret -n stockxpress -o yaml > mysql-secrets-plain.yml

# 3. Seal it
kubeseal -f mysql-secrets-plain.yml -w mysql-order-sealed-secret.yml

# 4. Delete old secret
kubectl delete secret mysql-order-secret -n stockxpress

# 5. Apply sealed secret
kubectl apply -f mysql-order-sealed-secret.yml

# 6. Verify
kubectl get secret mysql-order-secret -n stockxpress

# 7. Remove committed secret from Git
git rm k8s/base/infrastructure/mysql-secrets.yml
git commit -m "Remove committed secrets, use Sealed Secrets"
```

---

## Recommended Approach

### Development/Testing:
**Use Manual kubectl** - Simple, fast, good enough

### Staging with GitOps:
**Use Sealed Secrets** - Full GitOps support, audit trail

### Production:
**Use External Secrets Operator** - Enterprise-grade, rotation, compliance

---

## Summary

✅ **DO:**
- Use External Secrets Operator for production
- Use Sealed Secrets for GitOps workflows
- Use kubectl for local development
- Rotate secrets regularly
- Audit secret access
- Use strong, unique passwords

❌ **DON'T:**
- Commit plaintext secrets to Git
- Reuse passwords across environments
- Grant excessive permissions
- Store secrets in ConfigMaps
- Log secret values
- Share secrets via chat/email

---

**For StockXpress deployment, we recommend:**
1. **Local/Dev:** Manual kubectl creation
2. **CI/CD:** Sealed Secrets
3. **Production:** External Secrets Operator with AWS Secrets Manager

This ensures security at all stages without committing sensitive data to version control.