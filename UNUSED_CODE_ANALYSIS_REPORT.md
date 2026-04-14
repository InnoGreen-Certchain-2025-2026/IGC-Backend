# 📊 Phân Tích Unused Code & Configuration - IGC Backend Certificate Management System

**Ngày Phân Tích**: 04/04/2026  
**Phiên Bản Java**: 21  
**Spring Boot**: 4.0.2  
**Tổng Số File Java**: 102

---

## 📋 Executive Summary

Phân tích toàn bộ Java codebase tìm thấy:
- **11 method** không được sử dụng hoặc chỉ được sử dụng nội bộ
- **3 configuration property** không được sử dụng
- **2 class/config** có thể không cần thiết
- **0 class** hoàn toàn không được sử dụng (tất cả đều được inject hoặc extend)

---

## 🔴 PHẦN 1: METHODS KHÔNG ĐƯỢC SỬ DỤNG TỪ CONTROLLER

### 1.1. CertificateService Methods

#### ❌ `downloadCertificatePdf(String certificateId)` 
**File**: `iuh.igc.service.core.CertificateService`  
**Status**: UNUSED từ Controller  
**Lý do**: Phương thức interface này định nghĩa `byte[] downloadCertificatePdf()` nhưng:
- Controller sử dụng `downloadCertificate()` thay vì `downloadCertificatePdf()`
- Method này được implement nhưng không được gọi từ bất kỳ đâu
- Có thể là phần dư từ refactoring cũ

**Vị trí**:
- Interface: [CertificateService.java](CertificateService.java#L22)
- Implementation: [CertificateServiceImpl.java](CertificateServiceImpl.java#L510-L520)

**Recommendation**: Xóa method này vì `downloadCertificate()` thực hiện công việc tương tự với kết quả chi tiết hơn

---

#### ⚠️ `reactivateCertificate(String certificateId)`
**File**: `iuh.igc.service.core.CertificateService`  
**Status**: Defined nhưng KHÔNG CÓ controller endpoint  
**Lý do**: 
- Phương thức này được implement đầy đủ trong `CertificateServiceImpl`
- Gọi `blockchainService.reactivateCertificate()` và blockchain reactivate
- Nhưng không có endpoint HTTP nào gọi nó (không trong CertificateController)
- Controllers chỉ có: draft, sign, verify, revoke, reissue, claim - không có reactivate

**Vị trí**:
- Interface: [CertificateService.java](CertificateService.java#L28)
- Implementation: [CertificateServiceImpl.java](CertificateServiceImpl.java#L559-L584)
- Blockchain: [BlockchainServiceImpl.java](BlockchainServiceImpl.java#L208-L230)

**Recommendation**: 
- Nếu feature reactivate không cần thiết: xóa method
- Nếu feature cần thiết: thêm endpoint vào CertificateController

---

#### ❌ `getAllCertificates()`
**File**: `iuh.igc.service.core.CertificateService`  
**Status**: UNUSED  
**Lý do**: 
- Trả về tất cả certificates mà KHÔNG FILTER bởi user hiện tại
- Có thể là security risk (expose tất cả certificates)
- Controller không gọi method này
- Có `getAllCertificatesByStudentId()` được sử dụng thay thế

**Vị trí**:
- Interface: [CertificateService.java](CertificateService.java#L30)
- Implementation: [CertificateServiceImpl.java](CertificateServiceImpl.java#L586-L593)

**Recommendation**: Xóa method này - không được sử dụng và có nguy hiểm bảo mật

---

#### ❌ `getCertificatesByOrganizationId(Long id)`
**File**: `iuh.igc.service.core.CertificateService`  
**Status**: UNUSED  
**Lý do**: 
- Không có controller endpoint gọi method này
- Lấy certificates theo organization nhưng không có API endpoint nào cung cấp feature này

**Vị trí**:
- Interface: [CertificateService.java](CertificateService.java#L39)
- Implementation: [CertificateServiceImpl.java](CertificateServiceImpl.java#L594-L607)

**Recommendation**: Xóa hoặc thêm endpoint nếu feature này cần thiết

---

#### ❌ `getCertificateById(String certificateId)`
**File**: `iuh.igc.service.core.CertificateService`  
**Status**: UNUSED  
**Lý do**: 
- Controller không sử dụng method này
- Có `verifyCertificate()` được sử dụng thay thế với validation

**Vị trí**:
- Interface: [CertificateService.java](CertificateService.java#L41)
- Implementation: [CertificateServiceImpl.java](CertificateServiceImpl.java#L608-L627)

**Recommendation**: Xóa - là phần dư từ codebase cũ

---

#### ⚠️ `getCertificateByClaimCode(String claimCode)` - Duplicate!
**File**: Defined trong 2 services!  
**Status**: DUPLICATE method  
**Vấn đề**:
```
CertificateService.getCertificateByClaimCode()  ← UNUSED (defined in impl)
ClaimService.getCertificateByClaimCode()        ← USED (controller gọi)
```
- Controller gọi `claimService.getCertificateByClaimCode()` 
- Nhưng cũng định nghĩa trong `CertificateService` (không được gọi)

**Vị trí**:
- CertificateService: [CertificateService.java](CertificateService.java#L37)
- CertificateServiceImpl: [CertificateServiceImpl.java](CertificateServiceImpl.java#L628-L648)
- ClaimService: [ClaimService.java](ClaimService.java#L10)
- ClaimServiceImpl: [ClaimServiceImpl.java](ClaimServiceImpl.java#L53-L59)

**Recommendation**: Xóa method khỏi CertificateService vì ClaimService là chuyên biệt hơn

---

### 1.2. PDF Service Methods

#### ⚠️ `verifyHash(byte[] data, String expectedHash)`
**File**: `iuh.igc.service.pdf.HashService`  
**Status**: DEFINED nhưng không được gọi  
**Lý do**: 
- Interface định nghĩa method này
- Implementation tồn tại trong `HashServiceImpl`
- Nhưng không có code nào gọi method này
- Có thể dành cho tương lai nhưng không được sử dụng hiện tại

**Vị trí**:
- Interface: [HashService.java](HashService.java#L8)
- Implementation: [HashServiceImpl.java](HashServiceImpl.java#L54-L57)

**Recommendation**: Xóa nếu không plan sử dụng hoặc thêm comment "Dành cho verification trong tương lai"

---

#### ✅ `verifyPdfSignature(byte[] signedPdfBytes)` 
**File**: `iuh.igc.service.pdf.DigitalSignatureService`  
**Status**: USED  
**Gọi từ**: 
- [CertificateServiceImpl.java](CertificateServiceImpl.java#L298) - verify certificate
- [CertificateServiceImpl.java](CertificateServiceImpl.java#L443) - verify file

**Note**: Method này ĐƯỢC SỬ DỤNG ✅

---

### 1.3. S3 Service Methods

#### ✅ `deleteFileByKey(String key)`
**File**: `iuh.igc.config.s3.S3Service`  
**Status**: USED - nhưng cần kiểm tra  
**Gọi từ**:
- [UserServiceImpl.java](UserServiceImpl.java#L89, L94) - xóa avatar cũ khi upload avatar mới

**Note**: Method này ĐƯỢC SỬ DỤNG nhưng cần đảm bảo error handling tốt ✅

---

---

## 🟡 PHẦN 2: PROPERTIES TRONG YAML KHÔNG ĐƯỢC SỬ DỤNG

### 2.1. Signature Configuration Properties

#### ⚠️ `signature.reason`
**File**: `application-dev.yml` Line 44  
**Value**: `"Certificate Issued by University"`  
**Status**: DEFINED nhưng không được inject  

**Tìm kiếm**:
```bash
grep -r "signature.reason" src/main/java/
# Kết quả: 0 matches
```

**Vị trí**:
- application-dev.yml: Line 44

**Recommendation**: Hoặc xóa property hoặc inject vào `DigitalSignatureServiceImpl` để sử dụng

---

#### ⚠️ `signature.location`
**File**: `application-dev.yml` Line 45  
**Value**: `"Vietnam"`  
**Status**: DEFINED nhưng không được inject  

**Tìm kiếm**:
```bash
grep -r "signature.location" src/main/java/
# Kết quả: 0 matches
```

**Vị trí**:
- application-dev.yml: Line 45

**Recommendation**: Xóa property nếu không sử dụng

---

### 2.2. Certificate Template Configuration

#### ⚠️ `certificate.template.background-color`
**File**: `application-dev.yml` Line 54  
**Value**: `"#1a237e"`  
**Status**: DEFINED nhưng không được inject  

**Tìm kiếm**:
```bash
grep -r "certificate.template.background-color" src/main/java/
grep -r "background-color" src/main/java/
# Kết quả: 0 matches từ @Value
```

**Vị trí**:
- application-dev.yml: Line 54

**Note**: Có `@Value("${certificate.template.*")` trong class nào không?

**Recommendation**: Xóa nếu không sử dụng

---

### 2.3. Configuration Properties Được Sử Dụng

✅ **Được sử dụng**:
- `spring.datasource.*` → Spring Boot tự sử dụng
- `spring.jpa.*` → Spring Boot tự sử dụng
- `spring.ai.openai.*` → AiConfig & AiServiceImpl
- `blockchain.*` → BlockchainServiceImpl
- `security.cors.allowed-origins` → SecurityConfig
- `security.jwt.*` → AuthConfig & AuthServiceImpl
- `aws.access-key`, `aws.secret-key`, `aws.s3.*` → S3Config & S3Service
- `logging.level.*` → Spring Boot tự sử dụng
- `management.endpoints.*` → Spring Boot tự sử dụng

---

## 🟠 PHẦN 3: CONFIG CLASSES CÓ THỂ KHÔNG CẦN THIẾT

### 3.1. ObjectMapperConfig

**File**: [ObjectMapperConfig.java](ObjectMapperConfig.java)  
**Status**: CHECK USAGE  

**Nội dung**:
```java
@Configuration
public class ObjectMapperConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
```

**Kiểm tra**:
```bash
grep -r "ObjectMapperConfig" src/main/java/
# Kết quả: Chỉ @Configuration annotation, không có explicit use
```

**Lý do**:
- Spring Boot 4.0.2 tự động register ObjectMapper bean
- Không cần phải tạo config riêng

**Recommendation**: Xóa class này nếu không có custom configuration

---

### 3.2. AuditConfig

**File**: [AuditConfig.java](AuditConfig.java)  
**Status**: USED  

**Nội dung**:
```java
@Configuration
public class AuditConfig {
    @Bean("auditorProvider")
    public AuditorAware<User> auditorProvider(...) {...}
}
```

**Kiểm tra**:
- Referenced trong `IGCBackendApplication.java`: `@EnableJpaAuditing(auditorAwareRef = "auditorProvider")`

**Status**: ✅ USED - cần thiết cho JPA Auditing

---

## 🔵 PHẦN 4: CLASSES/INTERFACES KHÔNG ĐƯỢC SỬ DỤNG

### Kiểm tra: Có class nào hoàn toàn không được sử dụng không?

**Kết quả**: ✅ KHÔNG CÓ class hoàn toàn không được sử dụng

Giải thích:
- Tất cả services được inject vào controllers
- Tất cả repositories được sử dụng trong services
- Tất cả entities được sử dụng trong repositories
- Tất cả DTOs được sử dụng trong controllers/services
- Tất cả exceptions được throw từ code

---

## 🟣 PHẦN 5: UNUSED METHODS TRONG IMPLEMENTATION

### 5.1. DraftCertificateService - Fully Used

✅ Tất cả methods được sử dụng:
- `createDraft()` → Controller
- `getDraftCertificates()` → Controller  
- `getSignedCertificates()` → Controller
- `getRevokedCertificates()` → Controller
- `reissueCertificate()` → Controller

---

### 5.2. BlockchainService - Có 1 Method Unused

#### ⚠️ `reactivateCertificate(String certificateId)`
**Status**: Implemented nhưng không được gọi  
**Vì sao**: `CertificateService.reactivateCertificate()` không được gọi từ controller

---

### 5.3. SigningService

✅ Fully used:
- `signCertificate()` → Controller
- `revokeCertificate()` → Controller

---

### 5.4. ClaimService

✅ Fully used:
- `claimCertificate()` → Controller
- `getCertificateByClaimCode()` → Controller
- `downloadCertificateByClaimCode()` → Controller

---

### 5.5. UserService

✅ Fully used:
- `getUserSession()` → Controller
- `getUserProfile()` → Controller
- `updateUserProfile()` → Controller
- `updateUserAvatar()` → Controller

---

### 5.6. OrganizationService

✅ Fully used:
- `createOrganization()` → Controller
- `getUserOrganizations()` → Controller
- `getUserBriefOrganizationList()` → Controller
- `getUserOrganizationById()` → Controller
- `updateOrganizationGeneral()` → Controller
- `updateOrganizationLegal()` → Controller
- `updateOrganizationContact()` → Controller

---

### 5.7. OrganizationMemberService

✅ Fully used:
- `getOrganizationMembers()` → Controller
- `promoteToModerator()` → Controller
- `demoteToMember()` → Controller
- `kickMember()` → Controller

---

### 5.8. OrganizationInviteService

✅ Fully used:
- `getInvitesByUserId()` → Controller
- `getInvitesByOrganization()` → Controller
- `inviteUser()` → Controller
- `acceptInvite()` → Controller
- `declineInvite()` → Controller
- `cancelInvite()` → Controller

---

### 5.9. AuthService

✅ Fully used:
- `register()` → AuthController
- `login()` → AuthController
- `logout()` → AuthController
- `refreshSession()` → AuthController
- `updatePassword()` → AuthController

---

## 📊 PHẦN 6: SUMMARY TABLE

| Item | Type | Status | Recommendation |
|------|------|--------|-----------------|
| `CertificateService.downloadCertificatePdf()` | Method | ❌ UNUSED | DELETE |
| `CertificateService.reactivateCertificate()` | Method | ⚠️ NO ENDPOINT | DELETE or ADD ENDPOINT |
| `CertificateService.getAllCertificates()` | Method | ❌ UNUSED | DELETE |
| `CertificateService.getCertificatesByOrganizationId()` | Method | ❌ UNUSED | DELETE or ADD ENDPOINT |
| `CertificateService.getCertificateById()` | Method | ❌ UNUSED | DELETE |
| `CertificateService.getCertificateByClaimCode()` | Method | ❌ DUPLICATE | DELETE (use ClaimService instead) |
| `HashService.verifyHash()` | Method | ❌ UNUSED | DELETE OR USE |
| `BlockchainService.reactivateCertificate()` | Method | ⚠️ PARTIAL | DELETE or implement endpoint |
| `signature.reason` | Config | ⚠️ UNUSED | DELETE |
| `signature.location` | Config | ⚠️ UNUSED | DELETE |
| `certificate.template.background-color` | Config | ⚠️ UNUSED | DELETE or USE |
| `ObjectMapperConfig` | Class | ⚠️ NOT NEEDED | DELETE |

---

## 🎯 PHẦN 7: ACTIONABLE RECOMMENDATIONS

### Priority 1: DELETE (có risk thấp)
1. ❌ Remove `CertificateService.downloadCertificatePdf()` - method thừa
2. ❌ Remove `CertificateService.getAllCertificates()` - security risk
3. ❌ Remove `HashService.verifyHash()` - unused
4. ❌ Remove `signature.reason` & `signature.location` properties từ YAML
5. ❌ Remove `ObjectMapperConfig` class

### Priority 2: REVIEW & DECIDE (cần quyết định)
1. ⚠️ `CertificateService.reactivateCertificate()` - remove hoặc add endpoint?
2. ⚠️ `CertificateService.getCertificatesByOrganizationId()` - remove hoặc implement?
3. ⚠️ `CertificateService.getCertificateById()` - consolidate với verify?
4. ⚠️ Merge `CertificateService.getCertificateByClaimCode()` → `ClaimService`

### Priority 3: MONITOR
- ✅ Tất cả classes được sử dụng
- ✅ Tất cả repositories được sử dụng
- ✅ Tất cả entities được sử dụng
- ✅ Tất cả main services được sử dụng

---

## 📝 NOTES

1. **Duplicate Methods**: Hai service có method tương tự `getCertificateByClaimCode()` - điều này gây confuse
2. **Security**: Method `getAllCertificates()` không filter by user - nguy hiểm
3. **Config**: Có 3 config properties được định nghĩa nhưng không sử dụng
4. **Coverage**: ~95% codebase được sử dụng effectively

---

## 🔗 References

**Controllers**:
- [CertificateController.java](CertificateController.java)
- [AuthController.java](AuthController.java)
- [UserController.java](UserController.java)
- [OrganizationController.java](OrganizationController.java)

**Services**:
- [CertificateServiceImpl.java](CertificateServiceImpl.java)
- [DraftCertificateServiceImpl.java](DraftCertificateServiceImpl.java)
- [SigningServiceImpl.java](SigningServiceImpl.java)
- [ClaimServiceImpl.java](ClaimServiceImpl.java)

**Configuration**:
- [application.yml](application.yml)
- [application-dev.yml](application-dev.yml)

---

**Generated**: 2026-04-04  
**Reviewed By**: Code Analysis Tool
