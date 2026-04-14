Add-Type -AssemblyName System.Drawing
$inputFile = "d:\MiTFG\TFG_app\app\src\main\res\drawable\ic_parkinghole_logo.png"
$outputFile = "d:\MiTFG\TFG_app\app\src\main\res\drawable\ic_notification_silhouette.png"

$img = [System.Drawing.Image]::FromFile($inputFile)
$bmp = New-Object System.Drawing.Bitmap($img)

for ($x = 0; $x -lt $bmp.Width; $x++) {
    for ($y = 0; $y -lt $bmp.Height; $y++) {
        $pixel = $bmp.GetPixel($x, $y)
        if ($pixel.A -gt 0) {
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($pixel.A, 255, 255, 255))
        }
    }
}

$bmp.Save($outputFile, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
$img.Dispose()
Write-Output ("Icon created successfully at {0}" -f $outputFile)
