# Client Caller ID (Android)

When a client calls, a card pops up over the incoming-call screen with their
details from your Excel file. Tap **Open card** for all fields plus Call / WhatsApp buttons.
You can also search clients by name or phone inside the app.

## 1. Build the app (one time, about 5 minutes, on your PC)

1. Sign in at https://github.com (free account).
2. Click **+ → New repository**, name it `ClientCallerID`, choose **Private**, click **Create repository**.
3. On the new page click **uploading an existing file**.
4. Unzip `ClientCallerID.zip` on your PC, open the `ClientCallerID` folder, select **everything inside it**
   (including the `.github` folder) and drag it into the browser. Click **Commit changes**.
   If the `.github` folder didn't upload (the Actions tab shows no run): click **Add file → Create new file**,
   type the name `.github/workflows/build.yml`, paste the contents of that file from the zip, and commit.
5. Open the **Actions** tab. A "Build APK" run starts by itself; wait for the green tick (3–5 min).
6. The APK is now on the repository's **Releases** page (right side of the main page → "Client Caller ID").

## 2. Install on the Samsung phone

1. On the phone, open the repository's **Releases** page in Chrome (sign in to GitHub), tap `ClientCallerID.apk`.
2. Open the downloaded file. If asked, allow **Install unknown apps** for Chrome / My Files.
3. If Play Protect warns about an unknown developer, choose **More details → Install anyway**.

## 3. Set up in the app (steps are numbered on screen)

1. **Import Excel file** – pick your `.xlsx` (or `.csv`). Copy it to the phone first
   (Google Drive, email, or USB). The app shows which column it uses as the name and which as phone.
   Old `.xls` files: in Excel use *Save As → Excel Workbook (.xlsx)*.
2. **Allow phone & notification access** – allow all.
3. **Allow showing over other apps** – switch it on for Client Caller ID.
4. **Set as Caller ID app** – choose Client Caller ID as "Caller ID & spam app".
5. Tap **Test** to see what the popup looks like.

Samsung tip: Settings → Apps → Client Caller ID → Battery → **Unrestricted**.

To update the client list, just import the Excel file again.

## How it matches numbers

Column headers containing *phone, tel, mobile, טלפון, נייד, פלאפון, סלולרי* are used as phone columns
(if none, columns that look like phone numbers). A cell may hold several numbers separated by `/ , ;`.
`050-1234567`, `+972 50 123 4567` and a number cell `501234567` all match the same caller.

Your data stays on the phone only; nothing is uploaded anywhere.
