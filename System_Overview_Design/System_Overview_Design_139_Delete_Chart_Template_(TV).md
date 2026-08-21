# System_Overview_Design_139_Delete_Chart_Template_(TV)

## Processing Overview

---

The API deletes chart template information.

   1. Receive request information ("chart template name")

   2. Delete the "TV chart template master" with "chart template name" as the condition

   3. Return response information ("system datetime")

For request and response properties, etc., refer to the separate document "Peach API".

## Data Flow

```mermaid
sequenceDiagram
Actor  api as API
participant hstr as TV chart template master

api->>hstr:Request information ("chart template name") 
hstr-->>api: Result("system datetime")  
```

## List of Tables, etc.

---

| # | Table Name           | Table Caption                 | Schema | Reference (x) | Remarks |
|---|----------------------|------------------------------|----------|---------|------|
| 1 | m_tv_chart_templates | TV chart template master | plum     | x       | -    |


## Processing Details

1. token authentication

   Confirm the validity of the token.

   Refer to supplementary material "S-01. Login status check".

2. Validation check

   | Parameter | Required | Length | Range | Format (type, email, etc.) | Memo |
   |------------|------|------|------|--------------------|------|
   | name       | x    | 64   | -    | Character               |      |

   In case of error, return status code (`422`), message (`CODE:30020`).

3. Retrieve data from [1] that matches the following conditions

   - "Customer NO" matches the customer NO of Token.

   - "Chart template name" matches the parameter "name".

   If data cannot be retrieved, return status code (`404`), message (`CODE:30404`).
   
4. Delete the data of [1]

   Return `[system datetime]` as the response.


## External Configuration Information

---

## Update Conditions

---

## Revision History

---
| Update Date     | Updated By    | Update Content |
|------------|-----------|----------|
| 2024/08/07 | Tri Trinh | Newly created |
