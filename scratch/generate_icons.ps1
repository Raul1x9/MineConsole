Add-Type -AssemblyName System.Drawing

$sourceFile = "c:\Users\Admin\Desktop\MCconsole\MineConsole-iOS\Assets.xcassets\AppIcon.appiconset\app_icon_1024.png"
$targetDir = "c:\Users\Admin\Desktop\MCconsole\MineConsole-iOS"

$sizes = @(
    @{ Name = "AppIcon60x60@2x.png"; Width = 120; Height = 120 }
    @{ Name = "AppIcon60x60@3x.png"; Width = 180; Height = 180 }
    @{ Name = "AppIcon76x76@2x~ipad.png"; Width = 152; Height = 152 }
    @{ Name = "AppIcon83.5x83.5@2x~ipad.png"; Width = 167; Height = 167 }
    @{ Name = "AppIcon1024x1024.png"; Width = 1024; Height = 1024 }
)

foreach ($size in $sizes) {
    $destFile = Join-Path $targetDir $size.Name
    Write-Host "Resizing to $($size.Name) ($($size.Width)x$($size.Height))..."
    
    $srcImg = [System.Drawing.Image]::FromFile($sourceFile)
    $bmp = New-Object System.Drawing.Bitmap($size.Width, $size.Height)
    $graph = [System.Drawing.Graphics]::FromImage($bmp)
    
    $graph.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graph.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $graph.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    
    $graph.DrawImage($srcImg, 0, 0, $size.Width, $size.Height)
    
    $bmp.Save($destFile, [System.Drawing.Imaging.ImageFormat]::Png)
    
    $graph.Dispose()
    $bmp.Dispose()
    $srcImg.Dispose()
}
Write-Host "Icon generation completed!"
