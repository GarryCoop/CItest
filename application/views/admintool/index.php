<?php echo validation_errors(); ?>

<?php echo form_open('admtool/login') ?>

	<label for="name">Name</label>
	<input type="input" name="name" value="TEST"/><br />

	<label for="pwd">Password</label>
	<input name="pwd" value="tests" /><br />

	<input type="submit" name="submit" value="Login" />

</form>