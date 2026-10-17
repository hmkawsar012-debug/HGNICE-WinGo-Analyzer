HGNICE WinGo Analyzer — Android project

এই ZIP ফাইলটি নিজে APK নয়; এটি APK বানানোর সোর্স প্রজেক্ট। এই পরিবেশে Android SDK/Gradle ইনস্টল না থাকায় APK সরাসরি কম্পাইল করা যায়নি।

মোবাইল দিয়ে APK বানানোর সবচেয়ে সহজ উপায় (GitHub Actions):
1. github.com-এ সাইন ইন করুন। নতুন repository তৈরি করুন, নাম দিন HGNICE-WinGo-Analyzer। Public বা Private যেকোনোটি হতে পারে।
2. ZIP ফাইলটি ফোনে Extract করুন। extracted ফোল্ডারের ভেতরের ফাইলগুলো repository-তে upload করুন; .github ফোল্ডারটিও অবশ্যই থাকতে হবে।
3. GitHub repository-র Actions tab খুলুন। প্রথমবার চাইলে “I understand my workflows, go ahead and enable them” চাপুন।
4. “Build Android APK” workflow বেছে Run workflow চাপুন। সবুজ tick হলে workflow run খুলুন।
5. নিচের Artifacts অংশে HGNICE-WinGo-Analyzer-APK ডাউনলোড করুন। ZIP খুললে app-debug.apk পাবেন।
6. APK ফোনে খুলে install করুন। Android চাইলে browser/files app-এর জন্য “Install unknown apps” অনুমতি চাইতে পারে; শুধু নিজের তৈরি/বিশ্বাসযোগ্য APK-কে অনুমতি দিন। কাজ শেষে অনুমতি বন্ধ করে দিন।

সতর্কতা: APK শুধু আগের ফলাফল বিশ্লেষণ করে অনুমান দেখায়। HGNICE-এর আসল/পরবর্তী ফল জানে না, নিশ্চিত জেতার নিশ্চয়তা দেয় না। কোনো তৃতীয় পক্ষকে গেমের পাসওয়ার্ড, OTP বা টাকা দেবেন না।
