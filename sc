--[[
    BuzzHub Loader  (v.1.0)  -  ready to use
    Host THIS file publicly (your GitHub "sc" file). The hub itself lives in Junkie "Original Code".
    Never put your Junkie REST API key in this file.
]]

local CONFIG = {
    SERVICE    = "Fishing Simulator Script",
    IDENTIFIER = "1210752",
    PROVIDER   = "BuzzHub",
    SCRIPT_ID  = "99527de86e0dfaf0ad6a229ec4245c4d929856edb209ce60cc96ffe053d2978b",

    -- Set to true ONLY if your keys are one-time keys (check_key can consume them).
    ONE_TIME_KEYS = false,

    SAVE_FILE = "BuzzHubKey.txt"
}

local Players = game:GetService("Players")
local TweenService = game:GetService("TweenService")
local PlayerGui = Players.LocalPlayer:WaitForChild("PlayerGui")

print("[BuzzHub] loader started")
print(("[BuzzHub] service=[%s] provider=[%s] identifier=[%s]"):format(CONFIG.SERVICE, CONFIG.PROVIDER, CONFIG.IDENTIFIER))

----------------------------------------------------------------
-- Junkie SDK (key validation + protected script delivery only; the UI below is 100% custom)
----------------------------------------------------------------
local libOk, Junkie = pcall(function()
    return loadstring(game:HttpGet("https://jnkie.com/sdk/library.lua"))()
end)

local libError = nil
if not libOk or type(Junkie) ~= "table" then
    libError = "Couldn't load the key service: " .. tostring(Junkie)
    warn("[BuzzHub] " .. libError)
    Junkie = nil
else
    Junkie.service = CONFIG.SERVICE
    Junkie.identifier = CONFIG.IDENTIFIER
    Junkie.provider = CONFIG.PROVIDER
    Junkie.script_id = CONFIG.SCRIPT_ID
    print("[BuzzHub] key service loaded")
end

-- Runs the protected hub. Returns true on success, or false + error text.
local function launch(key)
    if not Junkie then return false, libError or "Key service unavailable." end
    getgenv().SCRIPT_KEY = key
    local ok, err = pcall(Junkie.load_script)
    if not ok then
        warn("[BuzzHub] load_script error: " .. tostring(err))
        return false, "Couldn't load the hub: " .. tostring(err)
    end
    return true
end

local function checkKey(key)
    key = (key or ""):gsub("%s+", "")
    if key == "" then return false, "Enter a key first.", key end
    if not Junkie then return false, libError or "Key service unavailable.", key end
    if CONFIG.ONE_TIME_KEYS then
        return true, "Loading...", key -- skip check_key so a one-time key isn't consumed
    end
    local ok, result = pcall(Junkie.check_key, key)
    if not ok then
        warn("[BuzzHub] check_key error: " .. tostring(result))
        return false, "Could not check the key: " .. tostring(result), key
    end
    if type(result) ~= "table" or not result.valid then
        return false, "Key rejected: " .. tostring(type(result) == "table" and result.error or "invalid"), key
    end
    return true, "Key accepted!", key
end

local function readSaved()
    local ok, data = pcall(function() return isfile(CONFIG.SAVE_FILE) and readfile(CONFIG.SAVE_FILE) or nil end)
    return ok and data or nil
end
local function writeSaved(key) pcall(function() writefile(CONFIG.SAVE_FILE, key) end) end

----------------------------------------------------------------
-- Custom key window (same look as the hub)
----------------------------------------------------------------
local Theme = {
    Bg = Color3.fromRGB(16, 17, 23), Elem = Color3.fromRGB(30, 33, 44), Accent = Color3.fromRGB(255, 190, 40),
    Text = Color3.fromRGB(236, 238, 245), SubText = Color3.fromRGB(150, 156, 175), Stroke = Color3.fromRGB(46, 50, 66),
    Good = Color3.fromRGB(90, 220, 130), Bad = Color3.fromRGB(255, 90, 90)
}

local function Create(class, props, parent)
    local o = Instance.new(class)
    for k, v in pairs(props or {}) do o[k] = v end
    if parent then o.Parent = parent end
    return o
end
local function Round(o, r) Create("UICorner", {CornerRadius = UDim.new(0, r or 6)}, o) end

local function showWindow(initialMsg)
    pcall(function() if getgenv().BuzzHubKeyGui then getgenv().BuzzHubKeyGui:Destroy() end end)
    local gui = Create("ScreenGui", {
        Name = "BuzzHubKeyGate", ResetOnSpawn = false, IgnoreGuiInset = true, DisplayOrder = 1000
    }, (gethui and gethui()) or PlayerGui)
    getgenv().BuzzHubKeyGui = gui

    local card = Create("CanvasGroup", {
        AnchorPoint = Vector2.new(0.5, 0.5), Position = UDim2.fromScale(0.5, 0.5),
        Size = UDim2.fromOffset(400, 260), BackgroundColor3 = Theme.Bg, BorderSizePixel = 0, GroupTransparency = 1
    }, gui)
    Round(card, 12)
    Create("UIStroke", {Color = Theme.Stroke, Thickness = 1}, card)
    local scale = Create("UIScale", {Scale = 0.9}, card)

    Create("Frame", {Size = UDim2.new(0, 60, 0, 3), Position = UDim2.new(0.5, -30, 0, 22), BackgroundColor3 = Theme.Accent, BorderSizePixel = 0}, card)
    Create("TextLabel", {
        Size = UDim2.new(1, 0, 0, 28), Position = UDim2.new(0, 0, 0, 36), BackgroundTransparency = 1,
        Text = "BuzzHub - Fishing Simulator", Font = Enum.Font.GothamBold, TextSize = 20, TextColor3 = Theme.Text
    }, card)
    Create("TextLabel", {
        Size = UDim2.new(1, 0, 0, 18), Position = UDim2.new(0, 0, 0, 64), BackgroundTransparency = 1,
        Text = "v.1.0  •  Free key, takes about a minute", Font = Enum.Font.Gotham, TextSize = 12, TextColor3 = Theme.Accent
    }, card)

    local box = Create("TextBox", {
        Size = UDim2.new(1, -48, 0, 38), Position = UDim2.new(0, 24, 0, 100), BackgroundColor3 = Theme.Elem,
        Text = "", PlaceholderText = "Paste your key here", PlaceholderColor3 = Theme.SubText,
        TextColor3 = Theme.Text, Font = Enum.Font.Gotham, TextSize = 13, ClearTextOnFocus = false, BorderSizePixel = 0
    }, card)
    Round(box, 6)

    local function button(text, x, primary)
        local b = Create("TextButton", {
            Size = UDim2.new(0.5, -30, 0, 36), Position = UDim2.new(x, x == 0 and 24 or 6, 0, 150),
            BackgroundColor3 = primary and Theme.Accent or Theme.Elem, Text = text, AutoButtonColor = true,
            TextColor3 = primary and Color3.fromRGB(20, 20, 20) or Theme.Text, Font = Enum.Font.GothamBold,
            TextSize = 13, BorderSizePixel = 0
        }, card)
        Round(b, 6)
        return b
    end
    local getBtn = button("Get Key", 0, false)
    local verifyBtn = button("Verify Key", 0.5, true)

    local status = Create("TextLabel", {
        Size = UDim2.new(1, -48, 0, 50), Position = UDim2.new(0, 24, 0, 198), BackgroundTransparency = 1,
        Text = "Click Get Key, finish the steps, then paste the key above.", Font = Enum.Font.Gotham,
        TextSize = 12, TextColor3 = Theme.SubText, TextWrapped = true, TextYAlignment = Enum.TextYAlignment.Top
    }, card)
    local function setStatus(t, c) status.Text = t; status.TextColor3 = c or Theme.SubText end
    if initialMsg then setStatus(initialMsg, Theme.Bad) end

    local gettingLink = false
    getBtn.MouseButton1Click:Connect(function()
        if gettingLink then return end
        gettingLink = true
        if not Junkie then
            setStatus(libError or "Key service unavailable.", Theme.Bad)
            gettingLink = false
            return
        end
        setStatus("Requesting your link...", Theme.SubText)
        local ok, link, err = pcall(Junkie.get_key_link)
        if not ok then
            setStatus("Key link error: " .. tostring(link), Theme.Bad)
        elseif not link then
            setStatus("Key link unavailable: " .. tostring(err), Theme.Bad)
        else
            local copied = pcall(function() setclipboard(link) end)
            setStatus(copied and "Link copied! Open it in your browser." or ("Open this link: " .. link), Theme.Accent)
        end
        gettingLink = false
    end)

    local busy = false
    verifyBtn.MouseButton1Click:Connect(function()
        if busy then return end
        busy = true
        setStatus("Checking key...", Theme.SubText)
        local ok, msg, key = checkKey(box.Text)
        if ok then
            writeSaved(key)
            setStatus(msg, Theme.Good)
            task.wait(0.5)
            TweenService:Create(card, TweenInfo.new(0.4), {GroupTransparency = 1}):Play()
            task.wait(0.45)
            gui:Destroy()
            local launched, lerr = launch(key)
            if not launched then showWindow(lerr) end
        else
            setStatus(msg, Theme.Bad)
            busy = false
        end
    end)

    TweenService:Create(card, TweenInfo.new(0.45), {GroupTransparency = 0}):Play()
    TweenService:Create(scale, TweenInfo.new(0.45, Enum.EasingStyle.Back), {Scale = 1}):Play()
end

----------------------------------------------------------------
-- START: try the saved key silently, otherwise show the window
----------------------------------------------------------------
local saved = readSaved()
if saved and not CONFIG.ONE_TIME_KEYS then
    local ok, _, key = checkKey(saved)
    if ok then
        local launched, lerr = launch(key)
        if launched then return end
        showWindow(lerr)
        return
    end
end
showWindow(libError)
