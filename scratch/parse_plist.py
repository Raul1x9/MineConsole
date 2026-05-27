import zipfile
import plistlib
import os

ipa_path = "MineConsole.ipa"
plist_path = "Payload/MineConsole.app/Info.plist"

if not os.path.exists(ipa_path):
    print(f"Error: {ipa_path} not found.")
    exit(1)

try:
    with zipfile.ZipFile(ipa_path, 'r') as zip_ref:
        if plist_path in zip_ref.namelist():
            plist_data = zip_ref.read(plist_path)
            plist_dict = plistlib.loads(plist_data)
            print("--- INFO.PLIST CONTENTS ---")
            for key, val in plist_dict.items():
                if "Icon" in key or "icon" in key or "CFBundle" in key:
                    print(f"{key}: {val}")
            print("\nFull CFBundleIcons:")
            print(plist_dict.get("CFBundleIcons"))
        else:
            print(f"Error: {plist_path} not found in zip.")
except Exception as e:
    print(f"Exception: {e}")
