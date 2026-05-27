import zipfile
import plistlib
import os

ipa_path = "Minecraft_1.21.131_13.0_LeTi.ipa"
if not os.path.exists(ipa_path):
    print(f"Error: {ipa_path} not found.")
    exit(1)

try:
    with zipfile.ZipFile(ipa_path, 'r') as zip_ref:
        files = zip_ref.namelist()
        print(f"Total files in IPA: {len(files)}")
        
        # Find Payload path
        app_dirs = [f for f in files if f.startswith("Payload/") and f.endswith(".app/")]
        if not app_dirs:
            # Let's search for any .app directories
            app_dirs = [f for f in files if ".app/" in f]
        
        if not app_dirs:
            print("Error: No .app directory found.")
            exit(1)
            
        app_dir = app_dirs[0]
        print(f"App directory: {app_dir}")
        
        # Check for Assets.car
        assets_car_path = app_dir + "Assets.car"
        has_assets_car = assets_car_path in files
        print(f"Has Assets.car: {has_assets_car}")
        
        # Check for PNG files matching Icon or AppIcon
        icon_files = [f for f in files if app_dir in f and (".png" in f or ".jpg" in f) and ("icon" in f.lower() or "appicon" in f.lower())]
        print(f"Found {len(icon_files)} icon files. First 15:")
        for idx, item in enumerate(icon_files[:15]):
            print(f" - {item}")
            
        # Parse Info.plist
        plist_path = app_dir + "Info.plist"
        if plist_path in files:
            plist_data = zip_ref.read(plist_path)
            plist_dict = plistlib.loads(plist_data)
            print("\n--- Minecraft Info.plist Icon Keys ---")
            for key, val in plist_dict.items():
                if "icon" in key.lower():
                    print(f"{key}: {val}")
            
            print("\nCFBundleIcons:")
            import pprint
            pprint.pprint(plist_dict.get("CFBundleIcons"))
            print("\nCFBundleIcons~ipad:")
            pprint.pprint(plist_dict.get("CFBundleIcons~ipad"))
        else:
            print(f"Error: Info.plist not found at {plist_path}")
            
except Exception as e:
    print(f"Exception: {e}")
