# NEXORA AI — Android ফোন থেকে APK ডাউনলোড ও Cloud Build গাইড

আপনার কাছে কম্পিউটার না থাকলেও শুধুমাত্র **Android ফোন** ব্যবহার করেই ২টি সহজ উপায়ে `NEXORA-AI-Debug.apk` ডাউনলোড ও ইনস্টল করতে পারবেন।

---

## পদ্ধতি ১: সরাসরি Google AI Studio থেকে (সবচেয়ে সহজ)

1. আপনার ফোনের ব্রাউজারে Google AI Studio প্রোজেক্টের উপরের **Settings / Export (⋮ বা ⚙️)** মেনুতে ট্যাপ করুন।
2. **Generate APK / AAB** (বা **Build APK**) অপশনটি নির্বাচন করুন।
3. বিল্ড শেষ হলে সরাসরি আপনার ফোনে `.apk` ফাইলটি ডাউনলোড হবে।

---

## পদ্ধতি ২: GitHub Actions Cloud Build (সরাসরি ডাউনলোড লিংকসহ)

এই প্রোজেক্টে `.github/workflows/build-apk.yml` যুক্ত করা হয়েছে, যা স্বয়ংক্রিয়ভাবে ক্লাউডে APK বিল্ড করে সরাসরি ডাউনলোড লিংক তৈরি করে দেবে:

1. Google AI Studio-র উপরের মেনু থেকে **Push to GitHub** বাটনে ট্যাপ করে একটি GitHub Repository-তে প্রোজেক্টটি পুশ করুন (ফোনের ব্রাউজার থেকেই এটি করা যায়)।
2. GitHub-এ আপনার Repository খুলে উপরের **Actions** ট্যাবে যান।
3. **Build NEXORA AI APK (Cloud Build)** ওয়ার্কফ্লোটি চলতে দেখবেন (৩–৫ মিনিট সময় লাগবে)। যদি নিজে থেকে চালু না হয়, তাহলে **Run workflow** বাটনে ট্যাপ করুন।
4. বিল্ড সফল (সবুজ টিক ✓) হলে নিচের যেকোনো এক জায়গা থেকে APK ডাউনলোড করুন:
   - **সরাসরি APK ফাইল (Unzip ছাড়াই):** Repository-এর **Releases** সেকশনে যান → `NEXORA AI Build #1` → **`NEXORA-AI-Debug.apk`**-এ ট্যাপ করলেই সরাসরি ফোনে ডাউনলোড হবে।
   - **অথবা Actions থেকে:** সম্পন্ন হওয়া বিল্ডের নিচে **Artifacts > NEXORA-AI-Debug-APK**-এ ট্যাপ করে ডাউনলোড করুন (এটি ZIP হিসেবে নামবে, ফোনের Files অ্যাপ দিয়ে Extract করলেই APK পাবেন)।

---

## Android ফোনে APK Install করার নিয়ম

1. ডাউনলোড করা **`NEXORA-AI-Debug.apk`** ফাইলে ট্যাপ করুন।
2. **"Install unknown apps"** অনুমতি চাইলে **Allow from this source** চালু করে দিন।
3. **Install** ট্যাপ করুন (Play Protect সতর্কতা এলে **More details > Install anyway** বেছে নিন, কারণ এটি আপনার নিজের তৈরি Debug APK)।
4. অ্যাপ ওপেন করে **Settings > Transparent Permission Center** থেকে Microphone, Overlay এবং Accessibility পারমিশনগুলো আপনার প্রয়োজন অনুযায়ী চালু করুন।
